import discord
from discord.ext import commands
from discord import app_commands
import os
import json
import asyncio
import time
import datetime
from datetime import timedelta
from threading import Thread
from http.server import HTTPServer, BaseHTTPRequestHandler
from typing import Optional, Union, Mapping

# --- Constants & Config ---
TOKEN = os.getenv("DISCORD_TOKEN", "")
PANELS_FILE = "ticket_panels.json"
CONFIG_FILE = "config.json"

def load_panels() -> dict:
    if os.path.exists(PANELS_FILE):
        with open(PANELS_FILE, "r") as f:
            try:
                return json.load(f)
            except:
                return {}
    return {}

def save_panels(data: dict):
    with open(PANELS_FILE, "w") as f:
        json.dump(data, f, indent=2)

def load_config():
    if os.path.exists(CONFIG_FILE):
        with open(CONFIG_FILE, "r") as f:
            try:
                return json.load(f)
            except:
                pass
    return {
        "curse_words": ["badword1", "badword2"],
        "curse_timeout": 10,
        "spam_timeout": 10,
        "spam_threshold": 4
    }

def save_config(config):
    with open(CONFIG_FILE, "w") as f:
        json.dump(config, f, indent=2)

# --- Web Server ---
class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'Discord Bot is running!')
    def log_message(self, format, *args):
        return

def run_server():
    try:
        httpd = HTTPServer(('0.0.0.0', 5000), SimpleHTTPRequestHandler)
        print("Web server running on port 5000")
        httpd.serve_forever()
    except Exception as e:
        print(f"Failed to start web server: {e}")

# --- Ticket Components ---
class CloseTicketView(discord.ui.View):
    def __init__(self):
        super().__init__(timeout=None)

    @discord.ui.button(label="Close Ticket", style=discord.ButtonStyle.danger, emoji="🔒", custom_id="ticket_close_button")
    async def close_ticket(self, interaction: discord.Interaction, button: discord.ui.Button):
        if not interaction.guild or not isinstance(interaction.channel, discord.TextChannel):
            return await interaction.response.send_message("This can only be used in a server text channel.", ephemeral=True)
            
        try:
            embed = discord.Embed(
                title="Ticket Closed",
                description=f"This ticket was closed by {interaction.user.mention}. Deleting in 3 seconds...",
                color=discord.Color.red()
            )
            await interaction.response.send_message(embed=embed)
            
            # Optional: Lock the channel so no one else can type while waiting
            overwrites: Mapping[Union[discord.Role, discord.Member, discord.Object], discord.PermissionOverwrite] = {
                interaction.guild.default_role: discord.PermissionOverwrite(view_channel=False),
                interaction.guild.me: discord.PermissionOverwrite(view_channel=True, send_messages=True, manage_channels=True)
            }
            try:
                await interaction.channel.edit(overwrites=overwrites)
            except:
                pass # Ignore if we can't edit permissions, we are deleting anyway

            await asyncio.sleep(3)
            await interaction.channel.delete()
        except discord.Forbidden:
            msg = "I don't have permission to manage/delete this channel."
            if not interaction.response.is_done():
                await interaction.response.send_message(msg, ephemeral=True)
            else:
                await interaction.followup.send(msg, ephemeral=True)

class QuestionModal(discord.ui.Modal):
    def __init__(self, panel_name: str, questions: list, category_id: Optional[int]):
        super().__init__(title=f"Ticket: {panel_name[:40]}")
        self.panel_name = panel_name
        self.category_id = category_id
        for q in questions[:5]:
            style = discord.TextStyle.paragraph if q.get("style") == "paragraph" else discord.TextStyle.short
            field = discord.ui.TextInput(
                label=q["label"][:45],
                style=style,
                placeholder=q.get("placeholder", "")[:100],
                required=q.get("required", True),
                max_length=1024
            )
            self.add_item(field)

    async def on_submit(self, interaction: discord.Interaction):
        if not interaction.guild: return
        category = interaction.guild.get_channel(self.category_id) if self.category_id else None
        
        overwrites: Mapping[Union[discord.Role, discord.Member, discord.Object], discord.PermissionOverwrite] = {
            interaction.guild.default_role: discord.PermissionOverwrite(view_channel=False),
            interaction.user: discord.PermissionOverwrite(view_channel=True, send_messages=True, attach_files=True, embed_links=True),
            interaction.guild.me: discord.PermissionOverwrite(view_channel=True, send_messages=True, manage_channels=True)
        }
        
        try:
            ticket_channel = await interaction.guild.create_text_channel(
                name=f"ticket-{interaction.user.name}"[:100],
                category=category if isinstance(category, discord.CategoryChannel) else None,
                overwrites=overwrites,
                reason=f"Ticket opened by {interaction.user}"
            )
            embed = discord.Embed(title=f"Ticket - {self.panel_name}", description=f"Ticket opened by {interaction.user.mention}", color=discord.Color.blue())
            for child in self.children:
                if isinstance(child, discord.ui.TextInput):
                    embed.add_field(name=child.label, value=child.value or "No response", inline=False)
            await ticket_channel.send(embed=embed, view=CloseTicketView())
            # Ping @everyone
            await ticket_channel.send("@everyone")
            await interaction.response.send_message(f"Your ticket has been created: {ticket_channel.mention}", ephemeral=True)
        except discord.Forbidden:
            await interaction.response.send_message("I don't have permission to create ticket channels or view the specified category.", ephemeral=True)

class OpenTicketView(discord.ui.View):
    def __init__(self, panel_name: str):
        super().__init__(timeout=None)
        self.panel_name = panel_name

    @discord.ui.button(label="Open Ticket", style=discord.ButtonStyle.primary, emoji="🎫", custom_id="ticket_open_button")
    async def open_ticket(self, interaction: discord.Interaction, button: discord.ui.Button):
        if not interaction.guild: return
        panels = load_panels()
        guild_panels = panels.get(str(interaction.guild_id), {})
        panel_data = guild_panels.get(self.panel_name)
        if not panel_data: return await interaction.response.send_message("Panel not found.", ephemeral=True)
        
        questions = panel_data.get("questions", [])
        category_id = panel_data.get("category_id")
        if questions:
            await interaction.response.send_modal(QuestionModal(self.panel_name, questions, category_id))
        else:
            category = interaction.guild.get_channel(category_id) if category_id else None
            overwrites: Mapping[Union[discord.Role, discord.Member, discord.Object], discord.PermissionOverwrite] = {
                interaction.guild.default_role: discord.PermissionOverwrite(view_channel=False),
                interaction.user: discord.PermissionOverwrite(view_channel=True, send_messages=True, attach_files=True, embed_links=True),
                interaction.guild.me: discord.PermissionOverwrite(view_channel=True, send_messages=True, manage_channels=True)
            }
            try:
                ticket_channel = await interaction.guild.create_text_channel(
                    name=f"ticket-{interaction.user.name}"[:100], 
                    category=category if isinstance(category, discord.CategoryChannel) else None, 
                    overwrites=overwrites
                )
                embed = discord.Embed(title=f"Ticket - {self.panel_name}", description=f"Ticket opened by {interaction.user.mention}", color=discord.Color.blue())
                await ticket_channel.send(embed=embed, view=CloseTicketView())
                # Ping @everyone
                await ticket_channel.send("@everyone")
                await interaction.response.send_message(f"Ticket created: {ticket_channel.mention}", ephemeral=True)
            except discord.Forbidden:
                await interaction.response.send_message("I don't have permission to create ticket channels.", ephemeral=True)

class QuestionTypeView(discord.ui.View):
    def __init__(self, panel_name: str, guild_id: str):
        super().__init__(timeout=300)
        self.panel_name = panel_name
        self.guild_id = guild_id

    def get_current_questions(self):
        panels = load_panels()
        return panels.get(self.guild_id, {}).get(self.panel_name, {}).get("questions", [])

    def create_embed(self):
        questions = self.get_current_questions()
        embed = discord.Embed(title=f"Configuring: {self.panel_name}", color=discord.Color.blue())
        if questions:
            q_list = "\n".join([f"{i+1}. {q['label']} ({q['style']})" for i, q in enumerate(questions)])
            embed.description = f"Current Questions:\n{q_list}"
        else:
            embed.description = "No questions added yet. Use buttons below to add."
        embed.set_footer(text=f"Max 5 questions | Current: {len(questions)}")
        return embed

    @discord.ui.button(label="Add Short Answer", style=discord.ButtonStyle.primary)
    async def short(self, interaction: discord.Interaction, button: discord.ui.Button):
        if len(self.get_current_questions()) >= 5:
            return await interaction.response.send_message("Max 5 questions reached.", ephemeral=True)
        await interaction.response.send_modal(AddQuestionModal(self.panel_name, "short", self.guild_id, self))

    @discord.ui.button(label="Add Paragraph", style=discord.ButtonStyle.primary)
    async def long(self, interaction: discord.Interaction, button: discord.ui.Button):
        if len(self.get_current_questions()) >= 5:
            return await interaction.response.send_message("Max 5 questions reached.", ephemeral=True)
        await interaction.response.send_modal(AddQuestionModal(self.panel_name, "paragraph", self.guild_id, self))

    @discord.ui.button(label="Clear All", style=discord.ButtonStyle.danger)
    async def clear(self, interaction: discord.Interaction, button: discord.ui.Button):
        panels = load_panels()
        if self.guild_id in panels and self.panel_name in panels[self.guild_id]:
            panels[self.guild_id][self.panel_name]["questions"] = []
            save_panels(panels)
        await interaction.response.edit_message(embed=self.create_embed(), view=self)

class AddQuestionModal(discord.ui.Modal):
    def __init__(self, panel_name: str, q_type: str, guild_id: str, parent_view: QuestionTypeView):
        super().__init__(title="Add Question")
        self.panel_name, self.q_type, self.guild_id, self.parent_view = panel_name, q_type, guild_id, parent_view
        self.label = discord.ui.TextInput(label="Question Text", max_length=45)
        self.placeholder = discord.ui.TextInput(label="Placeholder", required=False, max_length=100)
        self.add_item(self.label)
        self.add_item(self.placeholder)

    async def on_submit(self, interaction: discord.Interaction):
        panels = load_panels()
        panel = panels.get(self.guild_id, {}).get(self.panel_name)
        if not panel: return await interaction.response.send_message("Panel missing.", ephemeral=True)
        if "questions" not in panel: panel["questions"] = []
        panel["questions"].append({"label": self.label.value, "style": self.q_type, "placeholder": self.placeholder.value})
        save_panels(panels)
        await interaction.response.edit_message(embed=self.parent_view.create_embed(), view=self.parent_view)

# --- Main Bot Class ---
class MyBot(commands.Bot):
    def __init__(self):
        intents = discord.Intents.all()
        super().__init__(command_prefix='!', intents=intents, help_command=None)
        self.features = {"moderation": True, "tickets": True, "utility": True, "voice": True, "verification": True, "automation": True, "dm": False}
        self.config = load_config()
        self.curse_counts = {}
        self.spam_logs = {}

    async def setup_hook(self):
        panels = load_panels()
        for guild_id, guild_panels in panels.items():
            for panel_name in guild_panels:
                self.add_view(OpenTicketView(panel_name))
        self.add_view(CloseTicketView())
        
        # Global Error Handler
        @self.tree.error
        async def on_app_command_error(interaction: discord.Interaction, error: app_commands.AppCommandError):
            if isinstance(error, app_commands.MissingPermissions):
                await interaction.response.send_message("You don't have permission to use this command.", ephemeral=True)
            elif isinstance(error, app_commands.CommandInvokeError):
                original = error.original
                if isinstance(original, discord.Forbidden):
                    msg = "I don't have permission to do that. Please make sure my role is at the top of the list and has Administrator permissions."
                    if interaction.response.is_done():
                        await interaction.followup.send(msg, ephemeral=True)
                    else:
                        await interaction.response.send_message(msg, ephemeral=True)
                else:
                    msg = f"An error occurred: {str(original)}"
                    if interaction.response.is_done():
                        await interaction.followup.send(msg, ephemeral=True)
                    else:
                        await interaction.response.send_message(msg, ephemeral=True)
            else:
                if not interaction.response.is_done():
                    await interaction.response.send_message(f"An unexpected error occurred: {str(error)}", ephemeral=True)

        await self.tree.sync()
        print("Commands synced!")

    async def on_ready(self):
        print(f'Logged in as {self.user}')

    async def on_message(self, message):
        if message.author.bot or not message.guild: return
        if self.features.get("automation", True):
            content = message.content.lower()
            if any(word in content for word in self.config.get("curse_words", [])):
                try: await message.delete()
                except: pass
                uid = message.author.id
                self.curse_counts[uid] = self.curse_counts.get(uid, 0) + 1
                if self.curse_counts[uid] >= 2:
                    self.curse_counts[uid] = 0
                    try:
                        await message.author.timeout(timedelta(minutes=self.config.get("curse_timeout", 10)), reason="Curse threshold reached")
                        await message.channel.send(f"{message.author.mention} timed out for cursing.", delete_after=5)
                    except: pass
                return
            uid = message.author.id
            now = time.time()
            if uid not in self.spam_logs: self.spam_logs[uid] = []
            self.spam_logs[uid] = [t for t in self.spam_logs[uid] if now - t < 5]
            self.spam_logs[uid].append(now)
            if len(self.spam_logs[uid]) >= self.config.get("spam_threshold", 4):
                try: await message.delete()
                except: pass
                self.spam_logs[uid] = []
                try:
                    await message.author.timeout(timedelta(minutes=self.config.get("spam_timeout", 10)), reason="Spamming")
                    await message.channel.send(f"{message.author.mention} timed out for spamming.", delete_after=5)
                except: pass
                return
        await self.process_commands(message)

bot = MyBot()

# --- Commands ---
@bot.tree.command(name="info", description="What the bot does")
async def info(interaction: discord.Interaction):
    total_members = sum(g.member_count or 0 for g in bot.guilds)
    embed = discord.Embed(title="Bot Info", description="This bot is a multi-purpose tool for server management.", color=discord.Color.blue())
    embed.add_field(name="Features", value="• Moderation\n• Tickets\n• Voice\n• Automation\n• DM features\n• Verification system", inline=False)
    embed.add_field(name="Stats", value=f"Servers: {len(bot.guilds)}\nMembers: {total_members}\nLatency: {round(bot.latency * 1000)}ms", inline=False)
    await interaction.response.send_message(embed=embed)

@bot.tree.command(name="feature", description="Toggle features")
@app_commands.choices(feature=[
    app_commands.Choice(name="Moderation", value="moderation"),
    app_commands.Choice(name="Tickets", value="tickets"),
    app_commands.Choice(name="Utility", value="utility"),
    app_commands.Choice(name="Voice", value="voice"),
    app_commands.Choice(name="Automation", value="automation"),
    app_commands.Choice(name="DM Features", value="dm"),
    app_commands.Choice(name="Verification", value="verification")
])
@app_commands.checks.has_permissions(administrator=True)
async def feature_toggle(interaction, feature: str, enabled: bool):
    bot.features[feature] = enabled
    await interaction.response.send_message(f"Feature **{feature}** is now {'enabled' if enabled else 'disabled'}.", ephemeral=True)

@bot.tree.command(name="config", description="Configure detection")
@app_commands.checks.has_permissions(administrator=True)
async def config_cmd(interaction, curse_timeout: Optional[int] = None, spam_timeout: Optional[int] = None, spam_threshold: Optional[int] = None):
    if curse_timeout: bot.config["curse_timeout"] = curse_timeout
    if spam_timeout: bot.config["spam_timeout"] = spam_timeout
    if spam_threshold: bot.config["spam_threshold"] = spam_threshold
    save_config(bot.config)
    await interaction.response.send_message("Config updated!", ephemeral=True)

@bot.tree.command(name="ban")
@app_commands.checks.has_permissions(ban_members=True)
async def ban(interaction, member: discord.Member, reason: str = "None"):
    await member.ban(reason=reason); await interaction.response.send_message(f"Banned {member}", delete_after=10)

@bot.tree.command(name="timeout")
@app_commands.checks.has_permissions(moderate_members=True)
async def timeout(interaction, member: discord.Member, minutes: int, reason: str = "None"):
    await member.timeout(timedelta(minutes=minutes), reason=reason); await interaction.response.send_message(f"Timed out {member}", delete_after=10)

@bot.tree.command(name="servermute")
@app_commands.checks.has_permissions(mute_members=True)
async def servermute(interaction, member: discord.Member):
    await member.edit(mute=True); await interaction.response.send_message(f"Muted {member}", delete_after=10)

@bot.tree.command(name="unservermute")
@app_commands.checks.has_permissions(mute_members=True)
async def unservermute(interaction, member: discord.Member):
    await member.edit(mute=False); await interaction.response.send_message(f"Unmuted {member}", delete_after=10)

@bot.tree.command(name="serverdeafen")
@app_commands.checks.has_permissions(deafen_members=True)
async def serverdeafen(interaction, member: discord.Member):
    await member.edit(deafen=True); await interaction.response.send_message(f"Deafened {member}", delete_after=10)

@bot.tree.command(name="unserverdeafen")
@app_commands.checks.has_permissions(deafen_members=True)
async def unserverdeafen(interaction, member: discord.Member):
    await member.edit(deafen=False); await interaction.response.send_message(f"Undeafened {member}", delete_after=10)

@bot.tree.command(name="dm")
@app_commands.checks.has_permissions(administrator=True)
async def dm_cmd(interaction, member: discord.Member, message: str):
    if not bot.features.get("dm"): return await interaction.response.send_message("DM features are disabled.", ephemeral=True)
    try: await member.send(message); await interaction.response.send_message("Sent.", ephemeral=True, delete_after=10)
    except: await interaction.response.send_message("Failed.", ephemeral=True, delete_after=10)

@bot.tree.command(name="dmeveryone")
@app_commands.checks.has_permissions(administrator=True)
async def dmeveryone(interaction, message: str):
    if not bot.features.get("dm"): return await interaction.response.send_message("DM features are disabled.", ephemeral=True)
    if not interaction.guild: return
    await interaction.response.defer(ephemeral=True)
    for m in interaction.guild.members:
        if not m.bot: 
            try: await m.send(message)
            except: pass
    await interaction.followup.send("Done.", ephemeral=True)

@bot.tree.command(name="ticketpanel")
@app_commands.checks.has_permissions(administrator=True)
async def ticketpanel(interaction, title: str, description: str, category: Optional[discord.CategoryChannel] = None):
    panels = load_panels(); gid = str(interaction.guild_id)
    if gid not in panels: panels[gid] = {}
    panels[gid][title] = {"title": title, "description": description, "category_id": category.id if category else None, "questions": []}
    save_panels(panels)
    await interaction.channel.send(embed=discord.Embed(title=title, description=description), view=OpenTicketView(title))
    await interaction.response.send_message("Panel created.", ephemeral=True, delete_after=10)

@bot.tree.command(name="ticketpanelconfig", description="Add questions to a ticket panel")
@app_commands.checks.has_permissions(administrator=True)
async def ticketpanelconfig(interaction: discord.Interaction, panel_name: str):
    panels = load_panels()
    guild_id = str(interaction.guild_id)
    guild_panels = panels.get(guild_id, {})
    if panel_name not in guild_panels:
        return await interaction.response.send_message("Panel not found.", ephemeral=True)
    view = QuestionTypeView(panel_name, guild_id)
    await interaction.response.send_message(embed=view.create_embed(), view=view, ephemeral=True)

@ticketpanelconfig.autocomplete("panel_name")
async def panel_name_autocomplete(interaction: discord.Interaction, current: str) -> list[app_commands.Choice[str]]:
    panels = load_panels()
    guild_panels = panels.get(str(interaction.guild_id), {})
    return [app_commands.Choice(name=name, value=name) for name in guild_panels if current.lower() in name.lower()][:25]

@bot.tree.command(name="verify")
@app_commands.checks.has_permissions(manage_roles=True)
async def verify(interaction: discord.Interaction, member: discord.Member, role: discord.Role):
    try:
        await member.add_roles(role)
        await interaction.response.send_message(f"Verified {member.mention} and assigned {role.name}.", ephemeral=True, delete_after=10)
    except:
        await interaction.response.send_message("Failed to assign role.", ephemeral=True, delete_after=10)

if __name__ == "__main__":
    t = Thread(target=run_server); t.daemon = True; t.start()
    if TOKEN:
        bot.run(TOKEN)
    else:
        print("Missing DISCORD_TOKEN secret.")

This Minecraft Spigot / Paper plugin was created using Grok.

With this prompt:
Write a minecraft Bukkit plugin, that will allow players to save a home location or two. The number of homes a player can save is configurable. 
It should have the commands, /sethome [name] /listhomes /home [name] /deletehome [name] . If you only have one home, you can just use /home to go there. 
If you have multiple homes, the name is required, and show the player a list of options if they don't put in the name. When a player types /home [name] 
it will teleport them to that location. Make sure that the location is a safe spot. Don't teleport the player if there is lava , or they suffocate or fall. 
The plugin should use Maven to compile.


Maven building command: mvn clean package

Suggested software:
To replicate this yourself you will need some software.
Java Software Development Kit (SDK) version 17
https://adoptium.net/temurin/releases?version=17

Apache Maven
https://maven.apache.org/

Visual Code / Notepad++ / Notepad
https://code.visualstudio.com/

# Coon Gallery

Privacy-focused Android gallery with smart search support powered by metadata files.

<div align="center">
<img src="https://raw.githubusercontent.com/BOTPanzer/Coon-Gallery/refs/heads/main/screenshots/home.png" width="24%" />
<img src="https://raw.githubusercontent.com/BOTPanzer/Coon-Gallery/refs/heads/main/screenshots/search.png" width="24%" />
<img src="https://raw.githubusercontent.com/BOTPanzer/Coon-Gallery/refs/heads/main/screenshots/settings.png" width="24%" />
<img src="https://raw.githubusercontent.com/BOTPanzer/Coon-Gallery/refs/heads/main/screenshots/sync.png" width="24%" />
</div>

## Features

- Enable **smart search** with the power of **metadata files**.
  
  - Is there a cat in a photo? Just search for "cat" to find it!

- Wirelessly **backup your albums** to your computer.
  
  - Tired of relying on the cloud? Make local backups with the [PC app](https://github.com/BOTPanzer/Coon-Gallery-PC)!

- Multiple actions to **manage your albums**.
  
  - Everything you can find in other galleries: edit, share, move, copy, delete...

- Multiple **item selection** support.
  
  - Want to manage multiple files at a time? You can hold to select them!

- Android **trash & favourites API** support.
  
  - Trashed or favourited a file from another app? It will appear here too!

## How to Run

Choose whichever version you want from the [releases page](https://github.com/BOTPanzer/Coon-Gallery/releases) and download the apk or clone the repository and build it yourself using Android Studio.

## How to Use

Excluding the normal gallery features, there are some original features unique to Coon Gallery, these being **backing up your albums** to your PC and **enabling smart search** in the gallery. For these to work, you will need to set up *links* in the settings menu.

### Setup Links

*Links* are connections between an *album folder* and a *metadata file*.

- Adding an *album folder* will let you use the backup service to **backup your albums** from your phone to your computer.

- Adding an *album folder* and a *metadata file* will **improve the search** on those albums with the information inside the metadata file.

### Sync Albums & Metadata

To create **backups of your albums** and generate **metadata for their images**, you will need to connect to the [PC app](https://github.com/BOTPanzer/Coon-Gallery-PC) by following these steps:

1. **Find the connection code**
   
   To find the connection code, open the PC app and navigate to the sync menu.

2. **Connect using the code**
   
   Once you have the code, open the sync service in your gallery, type it into the "Code" input and press "Connect". From here, everything is managed by your computer.

3. **Sync your files**
   
   Once connected, you can manage what you want to sync from the PC app.

# PurgeBot
Written to purge user messages en masse.

To do:
- 
- Add confirmation for "all" command argument
- Tidy up code
- Improve logging for each deletion
- Look into replacing String references to Purge#0337
- ~~Separate token into external file (and include in gitignore)~~
- Better safeties for improper command usage
- ~~Make channelPurgeB allocate messages younger than two weeks to array sizes of 100 to take advantage of BulkDelete API~~
- ~~Create emote-based confirmation in command channel to command user~~

Known Issues:
- 
- Command message does not always delete itself
- Logged events are partially scrambled from intended order
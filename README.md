# PurgeBot
Written to purge user messages en masse.

To do:
- 
- Add verifier for "all" command argument
- Tidy up code
- ~~Improve logging for each deletion~~
- ~~Look into replacing String references to Purge#0337~~
- ~~Separate token into external file (and include in gitignore)~~
- Better safeties for improper command usage
- Consider storing Purger messages in a database or other, more reliable medium
- ~~Improve thread usage~~
- ~~Make channelPurgeB allocate messages younger than two weeks to array sizes of 100 to take advantage of BulkDelete API~~
- ~~Create emote-based verifier in command channel to command user~~
- ~~Run Purger channelPurge in separate thread from listener thread~~
- ~~Delay deletion logs to correspond with deletions~~
- ~~Consider re-merging old and new message arrays~~
- Build initialization command that reads/collects messages, separate from actual deletion command
- Get rate limit for deletion directly to time deletion requests

Known Issues:
- 
- ~~Command message does not always delete itself~~
    - Restarting discord causes the message to go away
- ~~Logged events are partially scrambled from intended order~~
- Deletion tasks sometimes freeze and rush to finish (but do not actually cause entire deletion to take any longer)
Glacier Freezer
---------------

** Purpose **

This will try to upload your .CR2 files (a minor modification would allow different behaviours...) to Amazon Glacier. Works on Ireland endpoint. Others are constants ready to use.

** Missing features **

This is unfinished (publish early...), but will eventually provide deduplication via SHA-1 hashing and a local (or S3?) storage of already frozen files. Will eventually provide a restore utility. And at some point in the feature, deal with the ugly multipart API to upload your nasty .MOV files...
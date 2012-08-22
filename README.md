Glacier Freezer
---------------

**Purpose**

This will try to upload your .CR2 files (a minor modification would allow different behaviours...) to Amazon Glacier. Works on Ireland endpoint. Others are constants ready to use. Makes use of your existing ~/.ec2/credentials.properties, which should be in the form:

    accessKey=<your-aws-access-key>
    secretKey=<your-aws-secret-key>

**Missing features**

This is unfinished (publish early...), but will eventually provide deduplication via SHA-1 hashing and a local (or S3?) storage of already frozen files. Will eventually provide a restore utility. And at some point in the feature, deal with the ugly multipart API to upload your nasty .MOV files...

**How to build**

I decided I'd give SBT a third chance to prove itself. So, try:

    sbt package

You should be left with a target/glacier-freezer-assembly-x.x.jar, ready to run using :
	java -jar target/glacier-freezer-assembly-x.x.jar

**Doesn't work?**

Feel free to fix it and send a pull request. I guarantee it works on my machine for my use case on my data. That's it.
Glacier Freezer
---------------

**Purpose**

This will try to upload your .NEF files (a minor modification would allow different behaviours...) to Amazon Glacier. Works on Ireland endpoint. Others are constants ready to use. Makes use of your existing ~/.ec2/credentials.properties, which should be in the form:

    accessKey=<your-aws-access-key>
    secretKey=<your-aws-secret-key>

**Done (nearly) features**

There is some SHA-1 deduplication using a cache file, stored as defined in your freezer.properties.
There is some (unfinished) inventory handing machinery, relying on your freezer.properties.

**Missing features**

This is unfinished (publish early...), but will eventually provide a restore utility. And at some point in the feature, deal with the ugly multipart API to upload your nasty .MOV files... And some day, store the cache file somewhere on S3.

**How to build**

I decided I'd give SBT a third chance to prove itself. So, try:

    sbt package

You should be left with a target/glacier-freezer-assembly-x.x.jar, ready to run using :
	java -jar target/glacier-freezer-assembly-x.x.jar

**How to configure**

The nice GUI will ask you some questions. Apart from that, there are two required files:
- The ~/.ec2/credentials.properties as discused in first paragraph
- The ~/.ec2/freezer.properties which should be in the same form as provided one (sits in scr/main/resources/freezer.properties), to configure stuff like cache path, SQS queue URL, and Vault name.

Your SNS queue url should point to an SQS queue, configured as the recipient of the SNS notification topic, which should itself be configured as the receiving end of your Vault's notifications for job completion. If you do not understand anything I just said, stay away from "check" option in the GUI, you will be safe. If you understand some of it, read Amazon documentation a bit further, and see "check" fail because it is unfinished work.

**Doesn't work?**

Feel free to fix it and send a pull request. I guarantee it works on my machine for my use case on my data. That's it.

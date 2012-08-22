package com.mathieubolla.glacierfreezer

import scala.collection.JavaConversions._
import com.amazonaws.{AmazonClientException, AmazonServiceException}
import com.amazonaws.auth.{AWSCredentials, PropertiesCredentials}
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.glacier.{AmazonGlacierClient, TreeHashGenerator}
import com.amazonaws.services.glacier.model.{DescribeVaultRequest, UploadArchiveRequest, JobParameters, InitiateJobRequest}
import com.amazonaws.util.BinaryUtils;

import java.io.{ByteArrayInputStream, InputStream, FileInputStream, FileOutputStream, File}

import Utils._

object Services {
	lazy val amazonCredentials = new PropertiesCredentials(new File(Constants.UserHomeCredentials))

	lazy val glacier = {
		val client = new AmazonGlacierClient(amazonCredentials)
		client.setEndpoint(Constants.Glacier(Constants.Ireland))
		client
	}

	lazy val sqs = {
		val client = new AmazonSQSClient(amazonCredentials)
		client.setEndpoint(Constants.Sqs(Constants.Ireland))
		client
	}

	def prepareInventory(vaultName:String) = {
			val jobParameters = new JobParameters()
				.withType("inventory-retrieval")
			val jobRequest = new InitiateJobRequest()
				.withJobParameters(jobParameters)
				.withVaultName(vaultName)
			Services.glacier.initiateJob(jobRequest).getJobId()
		}

	def dumpInventory(vaultName:String) = {
		val jobId = prepareInventory(vaultName)

		var receivedMessages = false;
		while (!receivedMessages) {
			for (message <- Services.sqs.receiveMessage(new ReceiveMessageRequest().withMaxNumberOfMessages(10).withQueueUrl(configFor("notifications.url"))).getMessages()) {
				Console.println("Received:\n" + message.getBody()+"\n")
				receivedMessages = true;
			}
			if (!receivedMessages) {
				Console.println("Didn't receive anything regarding job "+jobId)
				Thread.sleep(10000)
			}
		}
	}
}
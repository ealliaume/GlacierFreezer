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

import org.apache.commons.io.IOUtils
import Utils._
import Constants._

object Services {
	lazy val amazonCredentials = new PropertiesCredentials(new File(UserHomeCredentials))

	lazy val glacier = {
		val client = new AmazonGlacierClient(amazonCredentials)
		client.setEndpoint(Constants.Glacier(Ireland))
		client
	}

	lazy val sqs = {
		val client = new AmazonSQSClient(amazonCredentials)
		client.setEndpoint(Sqs(Ireland))
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

	def freeze(input:(File, String)) = {
			val inputFile = input._1
			val sha1 = input._2

			val byteArray = IOUtils.toByteArray(new FileInputStream(inputFile))
			val request = new UploadArchiveRequest().withVaultName(configFor("vault.name"))
				.withChecksum(TreeHashGenerator.calculateTreeHash(inputFile))
				.withBody(new ByteArrayInputStream(byteArray))
				.withContentLength(inputFile.length())

			try {
				(inputFile.getCanonicalPath, sha1, Some(Services.glacier.uploadArchive(request)))
			} catch {
				case e:Exception => {
					Console.println("Got an error with " + inputFile + "and SHA-1 " + sha1 + ". Error is "+e.getMessage())
					(inputFile.getCanonicalPath, sha1, None)
				}
			}
		}
}
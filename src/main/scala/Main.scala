package com.mathieubolla.glacierfreezer

import scala.collection.JavaConversions._
import org.apache.commons.io.IOUtils
import java.io.{ByteArrayInputStream, InputStream, FileInputStream, FileOutputStream, File}

import com.amazonaws.{AmazonClientException, AmazonServiceException}
import com.amazonaws.auth.{AWSCredentials, PropertiesCredentials}
import com.amazonaws.services.glacier.{AmazonGlacierClient, TreeHashGenerator}
import com.amazonaws.services.glacier.model.{DescribeVaultRequest, UploadArchiveRequest}
import com.amazonaws.util.BinaryUtils;

import java.security.DigestInputStream;
import java.security.MessageDigest;

import com.mathieubolla.glacierfreezer.Utils._

import scala.io.Source

object Glacier {
	def main(args:Array[String]) {
		lazy val configSupplier = () => {
			val computed = System.getProperty(Constants.UserCredentialsProperty, Constants.UserHomeCredentials)
			Console.println(computed)
			computed
		}

		lazy val amazonCredentials = new PropertiesCredentials(new File(configSupplier()))

		val client = () => {
			val client = new AmazonGlacierClient(amazonCredentials)
			client.setEndpoint(Constants.GlacierIreland)
			client
		}

		lazy val cache = {
			val CacheLine = "(.*)[\t](.*)[\t](.*)".r
			val Failure = "(.*)[\t](.*)[\t]failed".r
			val cache = new scala.collection.mutable.HashMap[String, String]
			for (line <- Source.fromFile("cache.dat").getLines()) {
				line match {
					case Failure(path, sha1) => Unit
					case CacheLine(path, sha1, archiveId) => {
						cache.put(sha1, archiveId)
					}
					case _ => Console.println("No decoder for " + line + ". Your cache.dat file seems to be corrupted.")
				}
			}
			cache
		}

		val glacier = (vaultName:String, inputFile:File, sha1:String) => {
			val byteArray = IOUtils.toByteArray(new FileInputStream(inputFile))
			val request = new UploadArchiveRequest().withVaultName(vaultName)
				.withChecksum(TreeHashGenerator.calculateTreeHash(inputFile))
				.withBody(new ByteArrayInputStream(byteArray))
				.withContentLength(inputFile.length())

			try {
				(inputFile.getCanonicalPath, sha1, Some(client().uploadArchive(request)))
			} catch {
				case e:Exception => {
					Console.println("Got an error with " + inputFile + "and SHA-1 " + sha1 + ". Error is "+e.getMessage())
					(inputFile.getCanonicalPath, sha1, None)
				}
			}
		}

		val get = (prompt:String, defaultValue:String) => {
			Console.print(prompt)
			Console.readLine() match {
				case "" => {
					defaultValue;
				}
				case value@_ => {
					value;
				}
			}
		}

		val results = walkFiles(new File(get("Photo path to upload: ", "/tmp/photo")))
			.filter(regularFilePath => regularFilePath.getCanonicalPath().endsWith(".CR2"))
			.map(regularFilePath => {
				val sha1DigestIS = new Sha1DigestInputStream(new FileInputStream(regularFilePath))
				IOUtils.toByteArray(sha1DigestIS)
				(regularFilePath, sha1DigestIS.getSha1())
			})
			.filter(pairOfPathAndSha1 => !cache.contains(pairOfPathAndSha1._2))
			.map(pairOfPathAndSha1 => glacier("Photos", pairOfPathAndSha1._1, pairOfPathAndSha1._2))

		results.filter(a => a._3.isDefined).map(a => appendToFile("cache.dat", a._1 + "\t" + a._2 + "\t" + a._3.get.getArchiveId()))
		results.filter(a => !a._3.isDefined).map(a => appendToFile("cache.dat", a._1 + "\t" + a._2 +"\tfailed"))
	}	
}

object Constants {
	val GlacierIreland = "https://glacier.eu-west-1.amazonaws.com/"
	val GlacierNorthernVirginia = "https://glacier.us-east-1.amazonaws.com/"
	val GlacierOregon = "https://glacier.us-west-2.amazonaws.com/"
	val GlacierNorthernCalifornia = "http://glacier.us-west-1.amazonaws.com/"
	val GlacierJapan = "https://glacier.ap-northeast-1.amazonaws.com/"
	val UserCredentialsProperty = "credentials.path"
	lazy val UserHomeCredentials = System.getProperty("user.home") + "/.ec2/credentials.properties"
}
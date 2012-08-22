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

		val loadCache = () => {
			val CacheLine = "(.*)[\t](.*)[\t](.*)".r
			val Failure = "(.*)[\t](.*)[\t]failed".r
			val cache = new scala.collection.mutable.HashMap[String, String]
			for (line <- Source.fromFile("cache.dat").getLines()) {
				line match {
					case Failure(path, sha1) => Unit
					case CacheLine(path, sha1, archiveId) => {
						cache.put(sha1, archiveId)
					}
					case _ => Console.println("No decoder for " + line)
				}
			}
			cache
		}

		val glacier = (vaultName:String, path:String) => {
			val archive = new File(path)
			val sha1DigestIS = new Sha1DigestInputStream(new FileInputStream(archive))
			val byteArray = IOUtils.toByteArray(sha1DigestIS)
			val sha1 = sha1DigestIS.getSha1()
			val request = new UploadArchiveRequest().withVaultName(vaultName)
				.withChecksum(TreeHashGenerator.calculateTreeHash(archive))
				.withBody(new ByteArrayInputStream(byteArray))
				.withContentLength(archive.length())

			try {
				(path, sha1, Some(client().uploadArchive(request)))
			} catch {
				case e:Exception => {
					(path, sha1, None)
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
			.filter(a => a.getCanonicalPath().endsWith(".CR2"))
			.map(a => glacier("Photos", a.getCanonicalPath))

		results.filter(a => a._3.isDefined).map(a => Console.println(a._1 + "\t" + a._2 + "\t" + a._3.get.getArchiveId()))
		results.filter(a => !a._3.isDefined).map(a => Console.println(a._1 + "\t" + a._2 +"\tfailed"))
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
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

object Glacier {
	def main(args:Array[String]) {
		lazy val children:(File) => Iterable[File] = (base:File) => {
			if (base.isDirectory) {
				base.listFiles.flatMap(element => children(element))
			} else {
				Seq(base)
			}
		}

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

		val results = children(new File(get("Photo path to upload: ", "/tmp/photo")))
			.filter(a => a.getCanonicalPath().endsWith(".CR2"))
			.map(a => glacier("Photos", a.getCanonicalPath))

		results.filter(a => a._3.isDefined).map(a => Console.println(a._1 + "\t" + a._2 + "\tok"))
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

class Sha1DigestInputStream(delegate:InputStream) extends DigestInputStream(delegate, MessageDigest.getInstance("SHA-1")) {
	def getSha1():String = {
		val hash = getMessageDigest().digest()
		val hexString = new StringBuilder()
		for (aHash <- hash) {
			val hex = Integer.toHexString(0xFF & aHash);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}
}
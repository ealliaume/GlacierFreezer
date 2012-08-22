package com.mathieubolla.glacierfreezer

import java.io.File
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.io.{InputStream, FileWriter, PrintWriter, FileInputStream}

import org.apache.commons.io.IOUtils

object Utils {
	def walkFiles:(File) => Iterable[File] = (base:File) => {
		if (base.isDirectory) {
			base.listFiles.flatMap(element => walkFiles(element))
		} else {
			Seq(base)
		}
	}

	def using[A <: {def close(): Unit}, B](param: A)(f: A => B): B = try { f(param) } finally { param.close() }

	def appendToFile(fileName:String, textData:String) = using(new FileWriter(fileName, true)) {
    	fileWriter => using(new PrintWriter(fileWriter)) {
    		printWriter => printWriter.println(textData)
    	}
  	}

	def getUserInput = (prompt:String, defaultValue:String) => {
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

	def computeSha1(file:File) = {
			val sha1DigestIS = new Sha1DigestInputStream(new FileInputStream(file))
			IOUtils.toByteArray(sha1DigestIS)
			sha1DigestIS.getSha1()
	}

	def configFor(propertyName:String) = {
		val properties = new java.util.Properties
		properties.load(new FileInputStream(new File(Constants.UserHomeConfig)))
		properties.getProperty(propertyName)
	}
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
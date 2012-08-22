package com.mathieubolla.glacierfreezer

import java.io.File
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.io.{InputStream, FileWriter, PrintWriter}

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
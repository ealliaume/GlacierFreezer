package com.mathieubolla.glacierfreezer

import com.mathieubolla.glacierfreezer.Utils._
import scala.io.Source
import java.io.File

object Glacier {
	lazy val cacheDestination = configFor("vault.cache")
	lazy val cache = {
			val CacheLine = "(.*)[\t](.*)[\t](.*)".r
			val Failure = "(.*)[\t](.*)[\t]failed".r
			val cache = new scala.collection.mutable.HashMap[String, String]
			for (line <- Source.fromFile(cacheDestination).getLines()) {
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

	def main(args:Array[String]) {
		getUserInput("What do you want to do now? [quit|upload|check]: ", "quit") match {
			case "upload" => {
				val results = walkFiles(new File(getUserInput("Where are your pictures? [/tmp/photo]: ", "/tmp/photo")))
					.filter(regularFile => regularFile.getCanonicalPath().endsWith(".CR2"))
					.map(regularFile => (regularFile, computeSha1(regularFile)))
					.filter(pairOfPathAndSha1 => !cache.contains(pairOfPathAndSha1._2))
					.map(pairOfPathAndSha1 => Services.freeze(pairOfPathAndSha1))

					results.filter(a => a._3.isDefined).map(a => appendToFile(cacheDestination, a._1 + "\t" + a._2 + "\t" + a._3.get.getArchiveId()))
					results.filter(a => !a._3.isDefined).map(a => appendToFile(cacheDestination, a._1 + "\t" + a._2 +"\tfailed"))
				}
			case "check" => {
				Services.dumpInventory(configFor("vault.name"))
			}

			case _ => System.exit(0)
		}
	}
}
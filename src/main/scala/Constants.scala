package com.mathieubolla.glacierfreezer

object Constants {
	val Ireland = "eu-west-1"
	val NorthernVirginia = "us-east-1"
	val NorthernCalifornia = "us-west-1"
	val Oregon = "us-west-2"
	val Japan = "ap-northeast-1"

	def Glacier(region:String) = "https://glacier." + region + ".amazonaws.com/"
	def Sqs(region:String) = "https://sqs." + region + ".amazonaws.com/"

	val UserCredentialsProperty = "credentials.path"
	lazy val UserHomeCredentials = System.getProperty("user.home") + "/.ec2/credentials.properties"
	lazy val UserHomeConfig = System.getProperty("user.home") + "/.ec2/freezer.properties"
}
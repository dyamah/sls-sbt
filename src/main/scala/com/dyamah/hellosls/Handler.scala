package com.dyamah.hellosls

import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider}
import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClient
import com.amazonaws.services.cloudwatchevents.model._
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.collection.JavaConversions._

object Handler {

	/** プロジェクトのルートディレクトリにある serverless.ymlを main/src/resources/ 以下にコピーしてjarファイルの同封されている。
		* 実行時にjarファイル内にあるリソースファイルから serverless.ymlを読み込み、必要な情報をcase class にシリアライズする
		*
		*/
	lazy val serverless: ServerLess = {
		Option(getClass.getClassLoader.getResourceAsStream("serverless.yml")) match {
			case Some(is) =>
				val mapper = new ObjectMapper(new YAMLFactory())
				mapper.registerModule(DefaultScalaModule)
				mapper.readValue(is, classOf[ServerLess])
			case _ =>
				throw new IllegalAccessException("Not found serverless.yml in .jar")
		}
	}


	val service: String = serverless.service

	val stage:String  = serverless.provider.stage

	import com.amazonaws.util.json.Jackson._
	implicit class ToJson(val req: Request) extends AnyVal {
		def toJson: String = toJsonString(req)
	}

}

/** AWS lambda の本体
	*
	*/
class Handler extends RequestHandler[Request, Response] {
	import Handler._

	/** lambdaの実行環境内に定義されている認証情報を使うおまじない
		*
		*/
	val provider = new AWSCredentialsProviderChain(new EnvironmentVariableCredentialsProvider())


	val region = Regions.AP_NORTHEAST_1

	/** CloudWatchEvents をlambda内で取り扱うクライアントを生成
		*
		*/
	val cwClient = new AmazonCloudWatchEventsClient(provider).withRegion(region).asInstanceOf[AmazonCloudWatchEventsClient]


	/** CloudWatch Events のRuleとそれに紐づく Target (lambda関数とわたす引数) を特定する
		*
		*/
	private def ruleAndTarget: (Option[Rule], Option[Target]) = {
		cwClient.listRules(new ListRulesRequest().withNamePrefix(s"$service-$stage")).getRules.headOption match {
			case Some(rule) =>
				cwClient.listTargetsByRule(new ListTargetsByRuleRequest().withRule(rule.getName)).getTargets.headOption match {
					case Some(target) =>
						(Some(rule), Some(target))
					case _ =>
						(Option(rule), None)
				}
			case _ =>
				(None, None)
		}
	}

	/** lambda関数本体: 引数で受け取ったrequestに対して input.count を1増やした入力を CloudWatch Events Rule のtargetに再設定する
		*
		* １分づつ 1づつカウントするlambda関数。これを応用すれば、lambda 関数の結果を引数に、自身を再帰的に呼び出す（n分毎）が実現できる。
		*
		* @param input 入力
		* @param context lambda関数を使うために必要なcontext オブジェクト
		*/
	def handleRequest(input: Request, context: Context): Response = {
		val logger = context.getLogger

		val message = ruleAndTarget match {
			case (Some(rule), Some(target)) =>
				logger.log(s"A pair of rule and target has been found: ${rule.getArn}-${target.getArn}")
				val nextReq = new Request(id = input.id, count = input.count + 1)
				target.setInput(nextReq.toJson)
				cwClient.putTargets(new PutTargetsRequest().withRule(rule.getName).withTargets(target))
				s"Next request is ${nextReq.toJson}"
			case (Some(rule), _) =>
				"A rule has been found, but no target of the rule"
			case _ =>
				"No rule has been found"
		}
		Response(message, input)
	}
}

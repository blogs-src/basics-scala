package demo

import com.typesafe.config.ConfigFactory

given timeout: Timeout = 30.seconds
// val config = ConfigFactory.load(System.getenv("APP_CONFIG_FILE"))

// var binding: Option[ServerBinding] = None
// var wallet: Option[ActorRef[Wallet.Command]] = None

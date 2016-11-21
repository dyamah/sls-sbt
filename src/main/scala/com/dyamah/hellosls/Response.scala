package com.dyamah.hellosls

import scala.beans.BeanProperty

/**
  * Created by dyama on 11/21/16.
  */
case class Response(@BeanProperty message: String, @BeanProperty request: Request)

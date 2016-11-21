package com.dyamah.hellosls

import scala.beans.BeanProperty

class Request(@BeanProperty var id: String, @BeanProperty var count: Int) {
  def this() = this("", 0)
}

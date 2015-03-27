package com.agilogy.srdb.test

import java.sql.Connection
import javax.sql.DataSource

import com.agilogy.srdb.tx.{ NewTransaction, TransactionController }
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec

import scala.collection.mutable.ArrayBuffer

class TransactionTest extends FlatSpec with MockFactory {

  val ds = mock[DataSource]
  val conn = mock[Connection]
  val txController = new TransactionController(ds)

  behavior of "Transaction"

  it should "apply commit callbacks on commit" in {
    val b = new ArrayBuffer[String]()
    (conn.isClosed _).expects().anyNumberOfTimes()
    inSequence {
      (() => ds.getConnection).expects().returns(conn)
      (conn.setAutoCommit _).expects(false)
      (conn.commit _).expects()
      (conn.close _).expects()
    }
    txController.inTransaction {
      tx =>
        tx.onCommit(() => b.append("committed"))
        assert(b.size === 0)
    }(NewTransaction)
    assert(b.toSeq === Seq("committed"))

  }

}

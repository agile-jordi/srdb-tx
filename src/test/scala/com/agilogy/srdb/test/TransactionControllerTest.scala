package com.agilogy.srdb.test

import java.sql.Connection
import javax.sql.DataSource

import com.agilogy.srdb.tx.{ NewTransaction, TransactionConfig, TransactionController }
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec

class TransactionControllerTest extends FlatSpec with MockFactory {

  behavior of "TransactionController"

  val conn = mock[Connection]
  val ds = mock[DataSource]
  val txController = new TransactionController(ds)

  it should "execute an empty transaction" in {
    (conn.isClosed _).expects().anyNumberOfTimes()
    inSequence {
      (() => ds.getConnection).expects().returns(conn)
      (conn.setAutoCommit _).expects(false)
      (conn.commit _).expects()
      (conn.close _).expects()
    }
    txController.inTransaction { tx => }(NewTransaction)
  }

  it should "abort a transaction when a exception is thrown and return such exception" in {
    (conn.isClosed _).expects().anyNumberOfTimes()
    inSequence {
      (() => ds.getConnection).expects().returns(conn)
      (conn.setAutoCommit _).expects(false)
      (() => conn.rollback()).expects()
      (conn.close _).expects()
    }
    val exc = intercept[RuntimeException] {
      txController.inTransaction {
        tx =>
          throw new RuntimeException("foo")
      }(NewTransaction)
    }
    assert(exc.getMessage === "foo")
  }

  def f1(txConfig: TransactionConfig): String = {
    txController.inTransaction(tx => "foo")(txConfig)
  }

  def f2(txConfig: TransactionConfig): String = {
    txController.inTransaction(tx => f1(tx) + "bar")(txConfig)

  }

  it should "propagate transactions" in {
    (conn.isClosed _).expects().anyNumberOfTimes()
    inSequence {
      (() => ds.getConnection).expects().returns(conn)
      (conn.setAutoCommit _).expects(false)
      (conn.commit _).expects()
      (conn.close _).expects()
    }
    assert(f2(NewTransaction) === "foobar")
  }

  it should "rollback the transaction (just once) if an exception is thrown" in {
    (conn.isClosed _).expects().anyNumberOfTimes()
    inSequence {
      (() => ds.getConnection).expects().returns(conn)
      (conn.setAutoCommit _).expects(false)
      (() => conn.rollback).expects()
      (conn.close _).expects()
    }
    val re = intercept[RuntimeException] {
      txController.inTransaction {
        tx =>
          txController.inTransaction(tx2 => throw new RuntimeException("ouch!"))(tx)
      }(NewTransaction)
    }
    assert(re.getMessage === "ouch!")
  }

}

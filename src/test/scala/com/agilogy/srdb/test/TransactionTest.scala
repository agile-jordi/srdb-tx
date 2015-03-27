package com.agilogy.srdb.test

import java.sql.{ Savepoint, Connection }
import javax.sql.DataSource

import com.agilogy.srdb.tx.{ Transaction, NewTransaction, TransactionController }
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec

import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

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

  it should "disallow inTransaction with an already closed transaction" in {
    (conn.isClosed _).expects().anyNumberOfTimes().returning(true)
    inSequence {
      (conn.setAutoCommit _).expects(false)
    }
    val tx = new Transaction(conn)
    val ise = intercept[IllegalStateException](txController.inTransaction(tx => ())(tx))
    assert(ise.getMessage === "Transaction already closed")
  }

  it should "allow to execute a block with a savepoint when no exceptions" in {
    inSequence {
      (conn.setAutoCommit _).expects(false)
      (() => conn.setSavepoint).expects()
    }
    val tx = new Transaction(conn)
    val res = tx.withSavepoint {
      "hey, ho!"
    } {
      case NonFatal(t) => "ouch!"
    }
    assert(res == "hey, ho!")
  }

  it should "allow to execute a block with a savepoint when uncaught exceptions" in {
    inSequence {
      (conn.setAutoCommit _).expects(false)
      (() => conn.setSavepoint).expects()
    }
    val tx = new Transaction(conn)
    val res = intercept[IllegalStateException] {
      tx.withSavepoint {
        throw new IllegalStateException("ouch!")
      } {
        case t: IllegalArgumentException => "yikes!"
      }
    }
    assert(res.getMessage === "ouch!")
  }

  it should "allow to execute a block with a savepoint when caught exceptions" in {
    val savepoint: Savepoint = mock[Savepoint]
    inSequence {
      (conn.setAutoCommit _).expects(false)
      (() => conn.setSavepoint).expects().returning(savepoint)
      (conn.rollback(_: Savepoint)).expects(savepoint)
    }
    val tx = new Transaction(conn)
    val res = tx.withSavepoint {
      throw new IllegalStateException("ouch!")
    } {
      case t: IllegalStateException => "Oh... ok"
    }
    assert(res === "Oh... ok")
  }

}

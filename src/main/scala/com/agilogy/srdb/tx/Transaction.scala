package com.agilogy.srdb.tx

import java.sql.Connection
import javax.sql.DataSource

import scala.collection.mutable.ListBuffer

sealed trait TransactionConfig

case object NewTransaction extends TransactionConfig

class Transaction(val conn: Connection) extends TransactionConfig{

  protected[this] var commitCallbacks: ListBuffer[() => Unit] = new ListBuffer()

  def onCommit(f: () => Unit): Unit = {
    commitCallbacks += f
  }

  conn.setAutoCommit(false)

  private[tx] def isClosed = conn.isClosed

  private[tx] def commit() {
    {
      try {
        if (conn.isClosed) throw new IllegalStateException("Connection already closed")
        conn.commit()
      } finally {
        close()
      }
    }
    commitCallbacks.foreach(f => f())
  }

  private def close() {
    try {
      if (!conn.isClosed) conn.close()
    } catch {
      case t: Throwable => t.printStackTrace()
    }
  }

  def rollback() {
    {
      try {
        if (!conn.isClosed) conn.rollback()
      } finally {
        close()
      }
    }
  }

  def withSavepoint[T](f: => T)(catchBlock:PartialFunction[Exception,T]):T = {
    val savepoint = this.conn.setSavepoint()
    try{
      f
    }catch{
      case e: Exception if catchBlock.isDefinedAt(e) =>
        this.conn.rollback(savepoint)
        catchBlock(e)
    }
  }


}

object Transaction {
  private[tx] def apply(ds: DataSource) = new Transaction(ds.getConnection)
}

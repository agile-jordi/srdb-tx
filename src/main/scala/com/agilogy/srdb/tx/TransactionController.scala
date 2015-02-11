package com.agilogy.srdb.tx

import javax.sql.DataSource

class TransactionController(ds:DataSource) {

  def inTransaction[T](f: Transaction => T)(implicit config: TransactionConfig): T = {
    require(config != null, "Don't ever use null again, please!")
    val (txCreated, tx) = config match {
      case NewTransaction => (true, Transaction(ds))
      case tx: Transaction => (false, tx)
    }
    try {
      val res = f(tx)
      // Transaction may be already closed if the user rolled it back by hand
      if (txCreated && !tx.isClosed) tx.commit()
      res
    } catch {
      case t: Throwable =>
        if (txCreated) {
          tx.rollback()
        }
        throw t
    }
  }
}

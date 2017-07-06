/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
 * Copyright (c) 2016 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.tp;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.db.Sql;
import org.simplity.kernel.db.SqlType;
import org.simplity.service.ServiceContext;

/**
 * execute a sql
 *
 * @author simplity.org
 */
public class ExecuteSql extends DbAction {
  static final Logger logger = Logger.getLogger(ExecuteSql.class.getName());

  /** qualified sql name */
  String sqlName;
  /** sheet name for input data */
  String inputSheetName;
  /**
   * many a times, we put constraints in db, and it may be convenient to use that to do the
   * validation. for example we insert a row, and db may raise an error because of a duplicate
   * columns. In such a case, we treat this error as "failure", rather than an exception
   */
  boolean treatSqlErrorAsNoResult;

  @Override
  protected int doDbAct(ServiceContext ctx, DbDriver driver) {
    Sql sql = ComponentManager.getSql(this.sqlName);
    if (this.inputSheetName == null) {
      return sql.execute(ctx, driver, this.treatSqlErrorAsNoResult);
    }

    DataSheet inSheet = ctx.getDataSheet(this.inputSheetName);
    if (inSheet != null) {
      return sql.execute(inSheet, driver, this.treatSqlErrorAsNoResult);
    }

    logger.log(
        Level.INFO,
        "Sql Save Action "
            + this.actionName
            + " did not execute because input sheet "
            + this.inputSheetName
            + " is not found.");
    Tracer.trace(
        "Sql Save Action "
            + this.actionName
            + " did not execute because input sheet "
            + this.inputSheetName
            + " is not found.");
    return 0;
  }

  @Override
  public DbAccessType getDataAccessType() {
    return DbAccessType.READ_WRITE;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.simplity.tp.DbAction#validate(org.simplity.kernel.comp.
   * ValidationContext
   * , org.simplity.tp.Service)
   */
  @Override
  public int validate(ValidationContext ctx, Service service) {
    int count = super.validate(ctx, service);
    if (this.sqlName == null) {
      ctx.addError("Sql name is required for ExecuteSql action");
      count++;
    } else {
      Sql sql = ComponentManager.getSqlOrNull(this.sqlName);
      if (sql == null) {
        ctx.addError("Sql " + this.sqlName + " is not defined");
        count++;
      } else if (sql.getSqlType() != SqlType.UPDATE) {
        ctx.addError(
            "Sql "
                + this.sqlName
                + " is designed for extracting data. It is not meant to be executed.");
        count++;
      }
    }
    ctx.addReference(ComponentType.SQL, this.sqlName);

    return count;
  }
}

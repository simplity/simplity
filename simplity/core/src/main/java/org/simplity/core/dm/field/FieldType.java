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
package org.simplity.core.dm.field;

/**
 * Different types of fields in a record as our data model. Note that every field in a view will
 * have the field type as view
 */
public enum FieldType {
  /** an attribute of this entity, holds data about this entity */
  DATA{
		@Override
		public boolean isDbField() {
			return false;
		}
  }

  /** simple data base column not a key or special field */
  ,
  DB_COLUMN

  /** primary key : normally internally and auto generated. example customerId */
  ,
  PRIMARY_KEY{
	@Override
	public boolean isPrimaryKey() {
		return true;
	}
  }

  /** link to its parent table */
  ,
  PARENT_KEY{
	@Override
	public boolean isParentKey() {
		return true;
	}
	@Override
	public boolean requiresReference() {
		return true;
	}
  }

  /** rare but useful setting when db designer uses composite keys */
  ,
  PRIMARY_AND_PARENT_KEY{
		@Override
		public boolean isPrimaryKey() {
			return true;
		}
		@Override
		public boolean isParentKey() {
			return true;
		}
		@Override
		public boolean requiresReference() {
			return true;
		}
	  
  }
  /** link to another table */
  ,
  FOREIGN_KEY{
	@Override
	public boolean requiresReference() {
		return true;
	}
  }

  /** created time stamp */
  ,
  CREATED_TIME_STAMP
  /** modified time stamp */
  ,
  MODIFIED_TIME_STAMP

  /** created by user */
  ,
  CREATED_BY_USER
  /** user id who modified it last */
  ,
  MODIFIED_BY_USER

  /**
   * data column of a view. Every column, except the key column in a view MUST have this as their
   * column type
   */
  ,
  VIEW{
		@Override
		public boolean requiresReference() {
			return true;
		}
  }

  /** This is an array of values. Valid only for records that represent data structure */
  ,
  VALUE_ARRAY{
	@Override
	public boolean isPrimitive() {
		return false;
	}
	@Override
	public boolean isDbField() {
		return false;
	}
  }
  /** this is a child-record. Valid only for records that represent data structure */
  ,
  RECORD{
		@Override
		public boolean isPrimitive() {
			return false;
		}
		
		@Override
		public boolean isDbField() {
			return false;
		}
	  }
  /** this is array of child-records. Valid only for records that represent data structure */
  ,
  RECORD_ARRAY{
		@Override
		public boolean isPrimitive() {
			return false;
		}
		@Override
		public boolean isDbField() {
		return false;
		}
	  }
  /**
   * key column that is put in all tables to save milti-tenant data in a single data base 
   * instead of creating separate database for each tenant
   */
  ,TENANT_KEY;
  /**
   * @return true if the type is primary or primary as well as parent
   */
  public boolean isPrimaryKey() {
	  return false;
  }
  /**
   * @return true if the type is parent key, or primary as well as parent
   */
  public boolean isParentKey() {
	  return false;
  }

  /**
   * @return true if the value is primitive. false if it represents an array or a data structure
   */
  public boolean isPrimitive() {
	  return true;
  }
  
  /**
   * 
   * @return does this field require a reference field
   */
  public boolean requiresReference() {
	  return false;
  }
 
  /**
   * 
   * @return is this field associated with a db
   */
  public boolean isDbField() {
	  return true;
  }
}

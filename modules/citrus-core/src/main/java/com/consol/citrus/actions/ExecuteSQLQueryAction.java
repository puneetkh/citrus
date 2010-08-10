/*
 * Copyright 2006-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.actions;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;

import com.consol.citrus.CitrusConstants;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.*;
import com.consol.citrus.functions.FunctionUtils;
import com.consol.citrus.variable.VariableUtils;

/**
 * Action executes SQL queries and offers result set validation. 
 * 
 * The class enables you to query data result sets from a
 * database. Validation will happen on column basis inside the result set.
 * 
 * It is possible to retry the action automatically. Test action will try to validate the
 * data a given number of times with configurable pause between the retries. 
 * 
 * This is especially helpful in case the system under test takes some time to save data 
 * to the database. Tests action may fail simply because of runtime conditions. With automatic retries
 * the test results are of stable nature.  
 *
 * @author Christoph Deppisch
 * @since 2008
 */
public class ExecuteSQLQueryAction extends AbstractDatabaseConnectingTestAction {
    /** Map holding all column values to be validated, keys represent the column names */
    protected Map<String, String> validationElements = new HashMap<String, String>();

    /** SQL file resource holding several query statements */
    private Resource sqlResource;

    /** List of SQL statements given inline inside the test case */
    private List<String> statements = new ArrayList<String>();

    /** Number of retries when validation fails */
    private int maxRetries = 0;

    /** Pause between retries (in milliseconds). */
    private int retryPauseInMs = 1000;
    
    /** Map saving db values to test variables, keys represent the column names, 
     * values the variable names */
    private Map<String, String> extractToVariablesMap = new HashMap<String, String>();

    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(ExecuteSQLQueryAction.class);

    /**
     * @see com.consol.citrus.TestAction#execute(TestContext)
     * @throws CitrusRuntimeException
     */
    @SuppressWarnings("unchecked")
	@Override
    public void execute(TestContext context) {
        BufferedReader reader = null;
        
        try {
            if (statements.isEmpty()) {
                reader = new BufferedReader(new InputStreamReader(sqlResource.getInputStream()));
                while (reader.ready()) {
                    String line = reader.readLine();
                    
                    if(line!= null) {
                        line = line.trim();
                        
                        if (!line.startsWith("--")) {
                            validateSqlStatement(line);

                            statements.add(line);
                        }
                    }
                }
            }

            Map<String, Object> resultMap = new HashMap<String, Object>();
            int countRetries = 0;
            boolean successful = false;
            while (!successful) {
                try {
                    Iterator<String> iter = statements.iterator();

                    while (iter.hasNext()) {
                        String stmt = iter.next();

                        try {
                            stmt = context.replaceDynamicContentInString(stmt);
                        } catch (ParseException e) {
                            log.error("Error while parsing sql statement: " + stmt);
                            throw new CitrusRuntimeException(e);
                        }
                        List list = getJdbcTemplate().queryForList(stmt);

                        checkOnResultSize(stmt, list);

                        resultMap.putAll((Map) list.get(0));
                    }

                    if (!validate(validationElements, resultMap, context)) {
                        throw new CitrusRuntimeException("Database validation failed");
                    }

                    successful = true;
                }
                catch (CitrusRuntimeException ex) {
                    if (countRetries >= maxRetries) {
                        throw ex;
                    }
                    log.warn("Validation failed. Retrying...");
                    countRetries++;
                    resultMap.clear();
                    try {
                        Thread.sleep(retryPauseInMs);
                    } catch (InterruptedException e) {
                        log.error("Unexpected interrupt.", e);
                    }
                }
            }

            for (Entry<String, Object> entry : resultMap.entrySet()) {
                if (entry.getValue() == null) {
                    resultMap.put(entry.getKey(), "NULL");
                }
            }

            //go through extract elements and save db values to variables
            for (Entry<String, String> entry : extractToVariablesMap.entrySet()) {
                String columnName = entry.getKey().toUpperCase();
                if(resultMap.containsKey(columnName)) {
                    context.setVariable(entry.getValue().toString(), resultMap.get(columnName).toString());
                } else {
                    throw new CitrusRuntimeException("Unable to find column '" + columnName + "' in database result set");
                }
            }

            //legacy: save all columns as variables TODO: remove in major version upgrade 
            Map<String, String> variableMap = new HashMap<String, String>();
            for (Entry<String, Object> entry : resultMap.entrySet()) {
                variableMap.put(CitrusConstants.VARIABLE_PREFIX + entry.getKey() + CitrusConstants.VARIABLE_SUFFIX, entry.getValue().toString());
            }

            context.addVariables(variableMap);
        } catch (IOException e) {
            log.error("File resource could not be found - filename: " + sqlResource.getFilename(), e);
            throw new CitrusRuntimeException(e);
        } catch (DataAccessException e) {
            log.error("Failed to execute SQL statement", e);
            throw new CitrusRuntimeException(e);
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("Error while closing reader instance.", e);
                }
            }
        }
    }


    /**
     * Checks on the size of the result set:
     * if no rows were returned a CitrusRuntimeException is thrown, if more than one row
     * is returned some logging entries are made, because we can only validate/save a single row.
     * 
     * @param stmt The SQL statement (just needed for logging).
     * @param resultList The list which is checked.
     */
	@SuppressWarnings("unchecked")
    private void checkOnResultSize(String stmt, List resultList) {
        if (resultList.size() == 0) {
            throw new CitrusRuntimeException("Validation not possible. SQL result set is empty for statement: " + stmt);
        }

        if (resultList.size()>1) {
            log.warn("Result set has more than one rows (" + resultList.size() + ") for statement: " + stmt);
            log.warn("Only first data row will be validated. Other rows in data set will be ignored!");

            if (log.isDebugEnabled()) {
                log.debug("Other data rows are:");
                for (int i=1; i<resultList.size(); i++) {
                    StringBuffer r = new StringBuffer();
                    Iterator it = ((Map) resultList.get(i)).entrySet().iterator();
                    while (it.hasNext()) {
                        Entry entry = (Entry) it.next();
                        String key = entry.getKey().toString();
                        r.append(key + " = " + entry.getValue() + "; \t");
                    }
                    log.debug(r.toString());
                }
            }
        }
    }

    /**
     * Does some simple validation on the SQL statement.
     * @param stmt The statement which is to be validated.
     */
    private void validateSqlStatement(String stmt) {
        if (!stmt.toLowerCase().startsWith("select")) {
            throw new CitrusRuntimeException("Missing keyword SELECT in statement: " + stmt);
        }

        int fromIndex = stmt.toLowerCase().indexOf("from");

        if (fromIndex <= "select".length()+1) {
            throw new CitrusRuntimeException("Missing keyword FROM in statement: " + stmt);
        }
    }

    /**
     * Validates the database result set. User can expect column names and respective values to be
     * present in the result set.
     * 
     * @param expectedValues user specified control result set
     * @param resultValues actual result set coming from the database
     * @return success flag
     * @throws UnknownElementException
     * @throws ValidationException
     */
    protected boolean validate(final Map<String, String> expectedValues, final Map<String, Object> resultValues, TestContext context) throws UnknownElementException, ValidationException
    {
        log.info("Start database query validation ...");

        for (Entry<String, String> entry : expectedValues.entrySet()) {
            String columnName = entry.getKey();
            String expectedValue = entry.getValue();
            
            if (!resultValues.containsKey(columnName)) {
                throw new CitrusRuntimeException("Could not find column " + columnName + " in SQL result set");
            }

            String columnValue = null;
            if (resultValues.get(columnName) != null) {
                columnValue = resultValues.get(columnName).toString();
            }

            if (VariableUtils.isVariableName(expectedValue)) {
                expectedValue = context.getVariable(expectedValue);
            } else if(context.getFunctionRegistry().isFunction(expectedValue)) {
                expectedValue = FunctionUtils.resolveFunction(expectedValue, context);
            } 

            //when validating databaseQuery null values are allowed
            if (columnValue == null) {
                if (expectedValue == null || expectedValue.toUpperCase().equals("NULL") || expectedValue.length() == 0) {
                    if(log.isDebugEnabled()) {
                        log.debug("Validating database value for column: " + columnName + " value as expected: NULL - value OK");
                    }
                } else {
                    throw new ValidationException("Validation failed for column: " +  columnName
                            + " found value: NULL expected value: " + expectedValue);
                }
            } else if (expectedValue != null && columnValue.equals(expectedValue)) {
                if(log.isDebugEnabled()) {
                    log.debug("Validation successful for column: " + columnName + " expected value: " + expectedValue + " - value OK");
                }
            } else {
                throw new ValidationException("Validation failed for column: " +  columnName
                        + " found value: '"
                        + columnValue
                        + "' expected value: "
                        + ((expectedValue == null || expectedValue.length()==0) ? "NULL" : expectedValue));
            }
        }

        log.info("Validation finished successfully: All values OK");

        return true;
    }

    /**
     * Setter for inline SQL statements.
     * @param statements
     */
    public void setStatements(List<String> statements) {
        this.statements = statements;
    }

    /**
     * Setter for external file resource holding the SQL statements.
     * @param sqlResource
     */
    public void setSqlResource(Resource sqlResource) {
        this.sqlResource = sqlResource;
    }

    /**
     * Set expected control result set. Keys represent the column names, values
     * the expected values.
     * 
     * @param validateDBValues
     */
    public void setValidationElements(Map<String, String> validationElements) {
        this.validationElements = validationElements;
    }

    /**
     * Setter for maximum number of retries.
     * @param maxRetries
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Retry interval.
     * @param retryPauseInMs
     */
    public void setRetryPauseInMs(int retryPauseInMs) {
        this.retryPauseInMs = retryPauseInMs;
    }


    /**
     * User can extract column values to test variables. Map holds column names (keys) and
     * respective target variable names (values).
     * 
     * @param extractToVariables the extractToVariables to set
     */
    public void setExtractToVariablesMap(Map<String, String> extractToVariablesMap) {
        this.extractToVariablesMap = extractToVariablesMap;
    }
}

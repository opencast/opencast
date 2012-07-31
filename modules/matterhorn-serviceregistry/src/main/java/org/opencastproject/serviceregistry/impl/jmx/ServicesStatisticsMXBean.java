/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.serviceregistry.impl.jmx;

/**
 * JMX Bean interface exposing service statistics.
 */
public interface ServicesStatisticsMXBean {

  /**
   * Gets a list of all services
   * 
   * @return an array including all services
   */
  String[] getServices();

  /**
   * Gets a list of all normal services
   * 
   * @return an array including all normal services
   */
  String[] getNormalServices();

  /**
   * Gets a list of all services in warning state
   * 
   * @return an array including all services in warning state
   */
  String[] getWarningServices();

  /**
   * Gets a list of all services in error state
   * 
   * @return an array including all services in error state
   */
  String[] getErrorServices();

  /**
   * Gets a list of all services of this JMX node
   * 
   * @return an array including all services of this JMX node
   */
  String[] getServicesByNode();

  /**
   * Gets a list of all normal services of this JMX node
   * 
   * @return an array including all normal services of this JMX node
   */
  String[] getNormalServicesByNode();

  /**
   * Gets a list of all services in warning state of this JMX node
   * 
   * @return an array including all services in warning state of this JMX node
   */
  String[] getWarningServicesByNode();

  /**
   * Gets a list of all services in error state of this JMX node
   * 
   * @return an array including all services in error state of this JMX node
   */
  String[] getErrorServicesByNode();

  /**
   * Gets the total number of services
   * 
   * @return the number of services
   */
  int getServiceCount();

  /**
   * Gets the total number of normal services
   * 
   * @return the number of normal services
   */
  int getNormalServiceCount();

  /**
   * Gets the total number of services in warning state
   * 
   * @return the number of services in warning state
   */
  int getWarningServiceCount();

  /**
   * Gets the total number of services in error state
   * 
   * @return the number of services in error state
   */
  int getErrorServiceCount();

  /**
   * Gets the number of services of this JMX node
   * 
   * @return the number of services of this JMX node
   */
  int getServiceCountByNode();

  /**
   * Gets the number of normal services of this JMX node
   * 
   * @return the number of normal services of this JMX node
   */
  int getNormalServiceCountByNode();

  /**
   * Gets the number of services in warning state of this JMX node
   * 
   * @return the number of services in warning state
   */
  int getWarningServiceCountByNode();

  /**
   * Gets the number of services in error state of this JMX node
   * 
   * @return the number of services in error state
   */
  int getErrorServiceCountByNode();

}

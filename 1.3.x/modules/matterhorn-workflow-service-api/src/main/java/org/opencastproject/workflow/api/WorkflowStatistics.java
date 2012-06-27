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
package org.opencastproject.workflow.api;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This class encapsualtes statistics for the workflow service.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "statistics", namespace = "http://workflow.opencastproject.org")
@XmlType(name = "statistics", namespace = "http://workflow.opencastproject.org")
public class WorkflowStatistics {

  /** The total number of workflow instances in the system */
  @XmlAttribute
  protected long total = 0;

  /** The total number of instantiated (not yet running) workflow instances in the system */
  @XmlAttribute
  protected long instantiated = 0;

  /** The total number of running workflow instances in the system */
  @XmlAttribute
  protected long running = 0;

  /** The total number of paused workflow instances in the system */
  @XmlAttribute
  protected long paused = 0;

  /** The total number of stopped workflow instances in the system */
  @XmlAttribute
  protected long stopped = 0;

  /** The total number of finished workflow instances in the system */
  @XmlAttribute
  protected long finished = 0;

  /** The total number of failing workflow instances in the system */
  @XmlAttribute
  protected long failing = 0;

  /** The total number of failed workflow instances in the system */
  @XmlAttribute
  protected long failed = 0;

  /** The workflow definition reports */
  @XmlElementWrapper(name = "definitions")
  @XmlElement(name = "definition")
  protected List<WorkflowDefinitionReport> definitions = new ArrayList<WorkflowStatistics.WorkflowDefinitionReport>();

  /**
   * @return the total
   */
  public long getTotal() {
    return total;
  }

  /**
   * @param total
   *          the total to set
   */
  public void setTotal(long total) {
    this.total = total;
  }

  /**
   * @return the instantiated
   */
  public long getInstantiated() {
    return instantiated;
  }

  /**
   * @param instantiated
   *          the instantiated to set
   */
  public void setInstantiated(long instantiated) {
    this.instantiated = instantiated;
  }

  /**
   * @return the running
   */
  public long getRunning() {
    return running;
  }

  /**
   * @param running
   *          the running to set
   */
  public void setRunning(long running) {
    this.running = running;
  }

  /**
   * @return the paused
   */
  public long getPaused() {
    return paused;
  }

  /**
   * @param paused
   *          the paused to set
   */
  public void setPaused(long paused) {
    this.paused = paused;
  }

  /**
   * @return the stopped
   */
  public long getStopped() {
    return stopped;
  }

  /**
   * @param stopped
   *          the stopped to set
   */
  public void setStopped(long stopped) {
    this.stopped = stopped;
  }

  /**
   * @return the finished
   */
  public long getFinished() {
    return finished;
  }

  /**
   * @param finished
   *          the finished to set
   */
  public void setFinished(long finished) {
    this.finished = finished;
  }

  /**
   * @return the failing
   */
  public long getFailing() {
    return failing;
  }

  /**
   * @param failing
   *          the failing to set
   */
  public void setFailing(long failing) {
    this.failing = failing;
  }

  /**
   * @return the failed
   */
  public long getFailed() {
    return failed;
  }

  /**
   * @param failed
   *          the failed to set
   */
  public void setFailed(long failed) {
    this.failed = failed;
  }

  /**
   * @return the definitions
   */
  public List<WorkflowDefinitionReport> getDefinitions() {
    return definitions;
  }

  /**
   * @param definitions
   *          the definitions to set
   */
  public void setDefinitions(List<WorkflowDefinitionReport> definitions) {
    this.definitions = definitions;
  }

  /**
   * Statistics for a specific workflow definition
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlRootElement(name = "definition_report", namespace = "http://workflow.opencastproject.org")
  @XmlType(name = "definition_report", namespace = "http://workflow.opencastproject.org")
  public static class WorkflowDefinitionReport {
    /** The workflow definition id */
    @XmlAttribute
    private String id;

    /** The total number of instances of this workflow definition */
    @XmlAttribute
    private long total = 0;

    /** The total number of instantiated (not yet running) instances of this workflow definition */
    @XmlAttribute
    private long instantiated = 0;

    /** The total number of running instances of this workflow definition */
    @XmlAttribute
    private long running = 0;

    /** The total number of paused instances of this workflow definition */
    @XmlAttribute
    private long paused = 0;

    /** The total number of stopped instances of this workflow definition */
    @XmlAttribute
    private long stopped = 0;

    /** The total number of finished instances of this workflow definition */
    @XmlAttribute
    private long finished = 0;

    /** The total number of failing instances of this workflow definition */
    @XmlAttribute
    private long failing = 0;

    /** The total number of failed instances of this workflow definition */
    @XmlAttribute
    private long failed = 0;

    /** The workflow operation reports */
    @XmlElementWrapper(name = "operations")
    @XmlElement(name = "operation")
    private List<OperationReport> operations = new ArrayList<WorkflowStatistics.WorkflowDefinitionReport.OperationReport>();

    /**
     * @return the id
     */
    public String getId() {
      return id;
    }

    /**
     * @param id
     *          the id to set
     */
    public void setId(String id) {
      this.id = id;
    }

    /**
     * @return the total
     */
    public long getTotal() {
      return total;
    }

    /**
     * @param total
     *          the total to set
     */
    public void setTotal(long total) {
      this.total = total;
    }

    /**
     * @return the instantiated
     */
    public long getInstantiated() {
      return instantiated;
    }

    /**
     * @param instantiated
     *          the instantiated to set
     */
    public void setInstantiated(long instantiated) {
      this.instantiated = instantiated;
    }

    /**
     * @return the running
     */
    public long getRunning() {
      return running;
    }

    /**
     * @param running
     *          the running to set
     */
    public void setRunning(long running) {
      this.running = running;
    }

    /**
     * @return the paused
     */
    public long getPaused() {
      return paused;
    }

    /**
     * @param paused
     *          the paused to set
     */
    public void setPaused(long paused) {
      this.paused = paused;
    }

    /**
     * @return the stopped
     */
    public long getStopped() {
      return stopped;
    }

    /**
     * @param stopped
     *          the stopped to set
     */
    public void setStopped(long stopped) {
      this.stopped = stopped;
    }

    /**
     * @return the finished
     */
    public long getFinished() {
      return finished;
    }

    /**
     * @param finished
     *          the finished to set
     */
    public void setFinished(long finished) {
      this.finished = finished;
    }

    /**
     * @return the failing
     */
    public long getFailing() {
      return failing;
    }

    /**
     * @param failing
     *          the failing to set
     */
    public void setFailing(long failing) {
      this.failing = failing;
    }

    /**
     * @return the failed
     */
    public long getFailed() {
      return failed;
    }

    /**
     * @param failed
     *          the failed to set
     */
    public void setFailed(long failed) {
      this.failed = failed;
    }

    /**
     * @return the operations
     */
    public List<OperationReport> getOperations() {
      return operations;
    }

    /**
     * @param operations
     *          the operations to set
     */
    public void setOperations(List<OperationReport> operations) {
      this.operations = operations;
    }

    /**
     * Statistics for a specific workflow operation within a given worflow definition
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "operation_report", namespace = "http://workflow.opencastproject.org")
    @XmlType(name = "operation_report", namespace = "http://workflow.opencastproject.org")
    public static class OperationReport {
      /** The workflow operation id */
      @XmlAttribute
      private String id;

      /** The total number of instances of this workflow definition currently in this operation */
      @XmlAttribute
      private long total = 0;

      /**
       * The total number of instantiated (not yet running) instances of this workflow definition currently in this
       * operation
       */
      @XmlAttribute
      private long instantiated = 0;

      /** The total number of running instances of this workflow definition currently in this operation */
      @XmlAttribute
      private long running = 0;

      /** The total number of paused instances of this workflow definition currently in this operation */
      @XmlAttribute
      private long paused = 0;

      /** The total number of stopped instances of this workflow definition currently in this operation */
      @XmlAttribute
      private long stopped = 0;

      /** The total number of finished instances of this workflow definition currently in this operation */
      @XmlAttribute
      private long finished = 0;

      /** The total number of failing instances of this workflow definition currently in this operation */
      @XmlAttribute
      private long failing = 0;

      /** The total number of failed instances of this workflow definition currently in this operation */
      @XmlAttribute
      private long failed = 0;

      /**
       * @return the id
       */
      public String getId() {
        return id;
      }

      /**
       * @param id
       *          the id to set
       */
      public void setId(String id) {
        this.id = id;
      }

      /**
       * @return the total
       */
      public long getTotal() {
        return total;
      }

      /**
       * @param total
       *          the total to set
       */
      public void setTotal(long total) {
        this.total = total;
      }

      /**
       * @return the instantiated
       */
      public long getInstantiated() {
        return instantiated;
      }

      /**
       * @param instantiated
       *          the instantiated to set
       */
      public void setInstantiated(long instantiated) {
        this.instantiated = instantiated;
      }

      /**
       * @return the running
       */
      public long getRunning() {
        return running;
      }

      /**
       * @param running
       *          the running to set
       */
      public void setRunning(long running) {
        this.running = running;
      }

      /**
       * @return the paused
       */
      public long getPaused() {
        return paused;
      }

      /**
       * @param paused
       *          the paused to set
       */
      public void setPaused(long paused) {
        this.paused = paused;
      }

      /**
       * @return the stopped
       */
      public long getStopped() {
        return stopped;
      }

      /**
       * @param stopped
       *          the stopped to set
       */
      public void setStopped(long stopped) {
        this.stopped = stopped;
      }

      /**
       * @return the finished
       */
      public long getFinished() {
        return finished;
      }

      /**
       * @param finished
       *          the finished to set
       */
      public void setFinished(long finished) {
        this.finished = finished;
      }

      /**
       * @return the failing
       */
      public long getFailing() {
        return failing;
      }

      /**
       * @param failing
       *          the failing to set
       */
      public void setFailing(long failing) {
        this.failing = failing;
      }

      /**
       * @return the failed
       */
      public long getFailed() {
        return failed;
      }

      /**
       * @param failed
       *          the failed to set
       */
      public void setFailed(long failed) {
        this.failed = failed;
      }
    }
  }

}

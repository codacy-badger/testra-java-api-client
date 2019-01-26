package tech.testra.java.client;

import static tech.testra.java.client.utils.PropertyHelper.prop;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import tech.testra.java.client.api.ExecutionApi;
import tech.testra.java.client.api.ProjectApi;
import tech.testra.java.client.api.ResultApi;
import tech.testra.java.client.api.ScenarioApi;
import tech.testra.java.client.api.SecurityScanApi;
import tech.testra.java.client.api.SimulationApi;
import tech.testra.java.client.api.TestcaseApi;
import tech.testra.java.client.model.EnrichedTestResult;
import tech.testra.java.client.model.ExecutionRequest;
import tech.testra.java.client.model.ScanResultRequest;
import tech.testra.java.client.model.Scenario;
import tech.testra.java.client.model.ScenarioRequest;
import tech.testra.java.client.model.SimulationRequest;
import tech.testra.java.client.model.TestResult;
import tech.testra.java.client.model.TestResultRequest;
import tech.testra.java.client.model.TestResultRequest.StatusEnum;
import tech.testra.java.client.model.Testcase;
import tech.testra.java.client.model.TestcaseRequest;
import tech.testra.java.client.utils.HostNameUtil;

@Slf4j
public final class TestraRestClient {

  private static String projectId;
  private static String executionIDString;
  private static ProjectApi projectApi = new ProjectApi();
  private static ScenarioApi scenarioApi = new ScenarioApi();
  private static ExecutionApi executionApi = new ExecutionApi();
  private static ResultApi resultApi = new ResultApi();
  private static SimulationApi simulationtApi = new SimulationApi();
  private static TestcaseApi testcaseApi = new TestcaseApi();
  public static SecurityScanApi securityScanApi = new SecurityScanApi();
  public static String buildRef;
  public static String execDesc;

  public static void setURLs(String url) {
    projectApi.getApiClient().setBasePath(url);
    executionApi.getApiClient().setBasePath(url);
    resultApi.getApiClient().setBasePath(url);
    scenarioApi.getApiClient().setBasePath(url);
    testcaseApi.getApiClient().setBasePath(url);
    simulationtApi.getApiClient().setBasePath(url);
    securityScanApi.getApiClient().setBasePath(url);

    if (prop("debugTestra") != null) {
      projectApi.getApiClient().setDebugging(true);
      executionApi.getApiClient().setDebugging(true);
      resultApi.getApiClient().setDebugging(true);
      scenarioApi.getApiClient().setDebugging(true);
      testcaseApi.getApiClient().setDebugging(true);
      simulationtApi.getApiClient().setDebugging(true);
      securityScanApi.getApiClient().setDebugging(true);
    }
  }

  public static String getProjectID(String projectName) {
    try {
      projectId = projectApi.getProject(projectName).getId();
      return projectId;
    } catch (ApiException e) {
      throw new IllegalArgumentException("Project not found in Testra");
    }
  }

  public static Scenario createScenario(ScenarioRequest scenarioRequest) {
    try {
      return scenarioApi.createScenario(projectId, scenarioRequest);
    } catch (ApiException e) {
      log.error("Error Creating Scenario " + scenarioRequest.getName());
      log.error(e.getResponseBody());
      e.printStackTrace();
      return null;
    }
  }

  public static Testcase createTestcase(TestcaseRequest testcaseRequest) {
    try {
      return testcaseApi.createTestcase(projectId, testcaseRequest);
    } catch (ApiException e) {
      log.error("Error Creating Test Case " + testcaseRequest.getName());
      log.error(e.getResponseBody());
      e.printStackTrace();
      return null;
    }
  }

  private static synchronized String createExecution() {
    if (executionIDString != null) {
      return executionIDString;
    }
    ExecutionRequest executionRequest = new ExecutionRequest();
    executionRequest.setParallel(false);
    executionRequest.setHost(HostNameUtil.hostName());
    executionRequest.setTags(Collections.singletonList(""));
    if (prop("branch") != null) executionRequest.setBranch(prop("branch"));
    if (prop("env") != null) executionRequest.setEnvironment(prop("env"));
    if (prop("execDesc") != null) executionRequest.setDescription(prop("execDesc"));
    if (prop("buildRef") != null) executionRequest.buildRef(prop("buildRef"));

    try {
      executionIDString = executionApi.createExecution(projectId, executionRequest).getId();
      return executionIDString;
    } catch (ApiException e) {
      log.error("Error Creating Execution");
      log.error(e.getResponseBody());
      e.printStackTrace();
      return null;
    }
  }

  public static TestResult createResult(TestResultRequest testResultRequest) {
    try {
      return resultApi.createResult(projectId, executionIDString, testResultRequest);
    } catch (ApiException e) {
      throw new RuntimeException("Creating execution in Testra failed");
    }
  }

  public static TestResult updateResult(String resultID, TestResultRequest testResultRequest) {
    try {
      return resultApi.updateResult(projectId, executionIDString, resultID, testResultRequest);
    } catch (ApiException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Could not update result");
    }
  }

  public static synchronized void setExecutionid(String eid) {
    if (eid == null) {
      createExecution();
    } else {
      executionIDString = eid;
    }
  }

  public static String getExecutionid() {
    return executionIDString;
  }

  public static List<EnrichedTestResult> getFailedResults() {
    return getResults(StatusEnum.FAILED.toString());
  }

  public static List<EnrichedTestResult> getResults(String resultType) {
    try {
      return resultApi.getResults(projectId, executionIDString, resultType);
    } catch (ApiException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("No results found with execution ID " + executionIDString);
    }
  }

  public static void createExecutionIDFile() {
    File file = new File("testra.exec");
    try (FileWriter writer = new FileWriter(file)) {
      file.createNewFile();
      writer.write(TestraRestClient.getExecutionid());
    } catch (IOException e) {
      log.error("Exception when creating execution file.", e);
    }
  }

  public static void createSimulation(SimulationRequest simulationRequest) {
    try {
      simulationtApi.createSimulation(projectId, executionIDString, simulationRequest);
    } catch (ApiException e) {
      log.error("Error Creating Simulation");
      log.error(e.getResponseBody());
      e.printStackTrace();
    }
  }

  public static void createSecurityScanResult(ScanResultRequest scanResultRequest) {
    try {
      securityScanApi.createSecurityScanResult(projectId, executionIDString, scanResultRequest);
    } catch (ApiException e) {
      log.error("Error Creating Security scan result");
      log.error(e.getResponseBody());
      e.printStackTrace();
    }
  }
}

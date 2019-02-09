package io.github.testra.java.client;

import io.github.testra.java.client.api.CounterApi;
import io.github.testra.java.client.api.ExecutionApi;
import io.github.testra.java.client.api.ProjectApi;
import io.github.testra.java.client.api.ResultApi;
import io.github.testra.java.client.api.ScenarioApi;
import io.github.testra.java.client.api.SecurityScanApi;
import io.github.testra.java.client.api.SimulationApi;
import io.github.testra.java.client.api.TestcaseApi;
import io.github.testra.java.client.model.EnrichedTestResult;
import io.github.testra.java.client.model.Execution;
import io.github.testra.java.client.model.ExecutionRequest;
import io.github.testra.java.client.model.Project;
import io.github.testra.java.client.model.ScanResult;
import io.github.testra.java.client.model.ScanResultRequest;
import io.github.testra.java.client.model.Scenario;
import io.github.testra.java.client.model.ScenarioRequest;
import io.github.testra.java.client.model.Simulation;
import io.github.testra.java.client.model.SimulationRequest;
import io.github.testra.java.client.model.TestResult;
import io.github.testra.java.client.model.TestResultRequest;
import io.github.testra.java.client.model.TestResultRequest.StatusEnum;
import io.github.testra.java.client.model.Testcase;
import io.github.testra.java.client.model.TestcaseRequest;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import retrofit2.Call;
import retrofit2.Response;

public final class TestraApiClient {

  private static String projectId;
  private static String executionId;

  private static ProjectApi projectApi;
  private static ScenarioApi scenarioApi;
  private static ExecutionApi executionApi;
  private static ResultApi resultApi;
  private static SimulationApi simulationApi;
  private static TestcaseApi testcaseApi;
  private static SecurityScanApi securityScanApi;

  private TestraApiClient() {}

  public static synchronized void init(Properties configs) {
    ApiClient apiClient = new ApiClient();

    if (System.getProperty("debugTestra") != null) {
      apiClient.configureFromOkclient(getLogInterceptedOkHttpClient());
    }
    String apiUrl = configs.getProperty("apiUrl");
    apiClient.setAdapterBuilder(apiClient.getAdapterBuilder().baseUrl(apiUrl));

    checkIsHostReachable(apiClient.createService(CounterApi.class), apiUrl);

    projectApi = apiClient.createService(ProjectApi.class);
    executionApi = apiClient.createService(ExecutionApi.class);
    resultApi = apiClient.createService(ResultApi.class);
    scenarioApi = apiClient.createService(ScenarioApi.class);
    testcaseApi = apiClient.createService(TestcaseApi.class);
    simulationApi = apiClient.createService(SimulationApi.class);
    securityScanApi = apiClient.createService(SecurityScanApi.class);

    setProject(configs.getProperty("project"));
  }

  private static void checkIsHostReachable(CounterApi counterApi, String apiUrl) {
    try {
      counterApi.getCounters().execute();
    } catch (IOException e) {
      throw new IllegalArgumentException(
          String.format("Testra api url (%s) is not reachable", apiUrl));
    }
  }

  private static OkHttpClient getLogInterceptedOkHttpClient() {
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.setLevel(Level.BODY);

    return new OkHttpClient.Builder().addInterceptor(logging).build();
  }

  private static void setProject(String projectName) {
    Response<Project> projectResponse = getResponse(() -> projectApi.getProject(projectName));

    if (projectResponse.isSuccessful()) {
      projectId = projectResponse.body().getId();
    } else {
      if (projectResponse.code() == 404) {
        throw new IllegalArgumentException("Project not found in Testra");
      }
    }
  }

  public static synchronized void createExecution(ExecutionRequest executionRequest) {
    if (executionId == null) {
      Execution execution =
          getResponseBody(() -> executionApi.createExecution(projectId, executionRequest));
      executionId = execution.getId();
    }
  }

  public static synchronized void setExecutionId(String executionId) {
    Response<Execution> executionResponse =
        getResponse(() -> executionApi.getExecution(projectId, executionId));

    if (executionResponse.isSuccessful()) {
      TestraApiClient.executionId = executionId;
    } else {
      if (executionResponse.code() == 404) {
        throw new IllegalArgumentException(
            String.format("Execution (id:%s) not found in Testra", executionId));
      }
    }
  }

  public static String getExecutionId() {
    return executionId;
  }

  public static Scenario createScenario(ScenarioRequest scenarioRequest) {
    return getResponseBody(() -> scenarioApi.createScenario(projectId, scenarioRequest));
  }

  public static Testcase createTestcase(TestcaseRequest testcaseRequest) {
    return getResponseBody(() -> testcaseApi.createTestcase(projectId, testcaseRequest));
  }

  public static TestResult createTestResult(TestResultRequest testResultRequest) {
    return getResponseBody(() -> resultApi.createResult(projectId, executionId, testResultRequest));
  }

  public static TestResult updateTestResult(
      String testResultId, TestResultRequest testResultRequest) {
    return getResponseBody(
        () -> resultApi.updateResult(projectId, executionId, testResultId, testResultRequest));
  }

  public static List<EnrichedTestResult> getFailedResults() {
    return getResults(StatusEnum.FAILED.toString());
  }

  public static List<EnrichedTestResult> getResults(String resultType) {
    return getResponseBody(() -> resultApi.getResults(projectId, executionId, resultType));
  }

  public static Simulation createSimulation(SimulationRequest simulationRequest) {
    return getResponseBody(
        () -> simulationApi.createSimulation(projectId, executionId, simulationRequest));
  }

  public static ScanResult createSecurityScanResult(ScanResultRequest scanResultRequest) {
    return getResponseBody(
        () -> securityScanApi.createSecurityScanResult(projectId, executionId, scanResultRequest));
  }

  private static <T> Response<T> getResponse(Supplier<Call<T>> supplier) {
    try {
      return supplier.get().execute();
    } catch (IOException e) {
      throw new TestraApiClientException(e);
    }
  }

  private static <T> T getResponseBody(Supplier<Call<T>> supplier) {
    Response<T> response = getResponse(supplier);
    if (response.isSuccessful()) {
      return response.body();
    } else {
      throw new TestraApiClientException(errorToString(response.errorBody()));
    }
  }

  private static String errorToString(ResponseBody errorResponseBody) {
    try {
      return errorResponseBody.string();
    } catch (IOException e) {
      throw new TestraApiClientException(e);
    }
  }
}

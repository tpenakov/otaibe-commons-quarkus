package otaibe.commons.quarkus.aws.rest.lambda.client.invoker.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyResponse;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import otaibe.commons.quarkus.aws.rest.lambda.client.invoker.RestLambdaInvoker;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

/*
 * Created by triphon 30.09.23 г.
 */
@RequiredArgsConstructor
@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
@Slf4j
public class RestLambdaInvokerImpl implements RestLambdaInvoker {

  private final LambdaClient client;
  private final ObjectMapper objectMapper;

  @Override
  public AwsProxyResponse invoke(final String arn, final AwsProxyRequest awsProxyRequest) {
    return Try.of(() -> getObjectMapper().writeValueAsString(awsProxyRequest))
        .map(requestBody -> SdkBytes.fromUtf8String(requestBody))
        .peek(requestBody -> log.trace("requestBody: {}", requestBody.asUtf8String()))
        .map(payload -> InvokeRequest.builder().functionName(arn).payload(payload).build())
        .flatMap(request -> Try.of(() -> client.invoke(request)))
        .peek(res -> log.trace("res: {}", res))
        .flatMap(res -> Try.of(() -> res.payload().asUtf8String()))
        .peek(response -> log.trace("response: {}", response))
        .flatMap(
            response -> Try.of(() -> getObjectMapper().readValue(response, AwsProxyResponse.class)))
        .get();
  }
}

package otaibe.commons.quarkus.aws.rest.lambda.client.invoker;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyResponse;

/*
 * Created by triphon 30.09.23 г.
 */
public interface RestLambdaInvoker {
  AwsProxyResponse invoke(final String arn, final AwsProxyRequest request);
}

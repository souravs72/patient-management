package com.convox.patientservice.grpc;


import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BillingServiceGrpcClient {

    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;

    // localhost:9001/BillingService/CreatePatientAccount          - localhost
    //aws.grpc:123123/BillingService/CreatePatientAccount          - aws
    public BillingServiceGrpcClient(@Value("${billing.service.address:localhost") String serverAddress,
                                    @Value("${billing.service.grpc.port:9001") int serverPort
    ) {
            log.info("Connecting to Billing Service Grpc service at {}:{} ", serverAddress, serverPort);

            ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort).usePlaintext().build();
            blockingStub = BillingServiceGrpc.newBlockingStub(channel);

    }

    public BillingResponse createBillingAccount(String patientId, String name, String email) {
        BillingRequest request = BillingRequest.newBuilder().setPatientId(patientId).setName(name).setEmail(email).build();
        BillingResponse response = blockingStub.createBillingAccount(request);

        log.info("Received response from Billing Service Grpc service via Grpc: {}", response);
        return response;
    }

}

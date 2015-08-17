package com.aplos.common.push;

import java.io.IOException;

import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.handler.OnMessage;

public class CalendarTest extends OnMessage<String> {

    @Override
    public void onMessage(AtmosphereResponse response, String message) throws IOException {
        response.write(message);
    }
}
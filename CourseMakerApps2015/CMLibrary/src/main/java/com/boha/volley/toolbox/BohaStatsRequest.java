package com.boha.volley.toolbox;

import android.util.Log;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.boha.coursemaker.dto.StatsResponseDTO;
import com.google.gson.Gson;
import org.apache.http.util.ByteArrayBuffer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BohaStatsRequest extends Request<StatsResponseDTO> {

    private Listener<StatsResponseDTO> listener;
    private ErrorListener errorListener;
    private long start, end;

    public BohaStatsRequest(int method, String url, ErrorListener listener) {
        super(method, url, listener);
    }

    public BohaStatsRequest(int method, String url,
                            Listener<StatsResponseDTO> responseListener, ErrorListener errorListener) {
        super(method, url, errorListener);
        this.listener = responseListener;
        this.errorListener = errorListener;
        start = System.currentTimeMillis();
        Log.i(LOG, "...Cloud Server communication started ...");

    }

    @Override
    protected Response<StatsResponseDTO> parseNetworkResponse(
            NetworkResponse response) {
        StatsResponseDTO dto = new StatsResponseDTO();
        Log.d(LOG, "parsing response, elapsed since start: "
                + getElapsed(start, System.currentTimeMillis()) + " seconds");
        try {
            Gson gson = new Gson();
            String resp = new String(response.data);
            Log.i(LOG, "response string length returned: " + resp.length());
            try {
                dto = gson.fromJson(resp, StatsResponseDTO.class);
                if (dto != null) {
                    return Response.success(dto,
                            HttpHeaderParser.parseCacheHeaders(response));
                }
            } catch (Exception e) {
                // ignore, it's a zipped response
            }

            InputStream is = new ByteArrayInputStream(response.data);
            ZipInputStream zis = new ZipInputStream(is);
            @SuppressWarnings("unused")
            ZipEntry entry;
            ByteArrayBuffer bab = new ByteArrayBuffer(4096);

            while ((entry = zis.getNextEntry()) != null) {
                int size = 0;
                byte[] buffer = new byte[4096];
                while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
                    bab.append(buffer, 0, size);
                }
                resp = new String(bab.toByteArray());
                dto = gson.fromJson(resp, StatsResponseDTO.class);
            }
        } catch (Exception e) {
            VolleyError ve = new VolleyError("Exception parsing server data", e);
            errorListener.onErrorResponse(ve);
            Log.e(LOG, "Unable to complete request: " + dto.getMessage(), e);
            return Response.error(new VolleyError(dto.getMessage()));
        }
        return Response.success(dto,
                HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(StatsResponseDTO response) {
        end = System.currentTimeMillis();
        Log.i(LOG, "---- Comms elapsed time: "
                + getElapsed(start, end) + " seconds");
        listener.onResponse(response);
    }

    public static double getElapsed(long start, long end) {
        BigDecimal m = new BigDecimal(end - start).divide(new BigDecimal(1000));
        return m.doubleValue();
    }

    static final String LOG = "BohaStatsRequest";
}

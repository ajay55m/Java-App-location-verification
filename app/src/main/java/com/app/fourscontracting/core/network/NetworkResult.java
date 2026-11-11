package com.app.fourscontracting.core.network;

/**
 * Generic wrapper for handling network states across repositories and presentation layer.
 */
public class NetworkResult<T> {
    public enum Status { SUCCESS, ERROR, LOADING }

    public final Status status;
    public final T data;
    public final String message;

    private NetworkResult(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> NetworkResult<T> success(T data) {
        return new NetworkResult<>(Status.SUCCESS, data, null);
    }

    public static <T> NetworkResult<T> error(String message, T data) {
        return new NetworkResult<>(Status.ERROR, data, message);
    }

    public static <T> NetworkResult<T> loading() {
        return new NetworkResult<>(Status.LOADING, null, null);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}


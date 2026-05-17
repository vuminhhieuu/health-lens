import { useState, useCallback } from 'react';
import { AxiosError } from 'axios';
import { apiClient } from '../lib/api/apiClient';
import { API_ROUTES } from '@/lib/api/routes';
import { getApiErrorCode, messageCatalog } from '@/lib/i18n/messages';
import { notify } from '@/lib/notify';

interface DeleteAccountResponse {
    data: {
        message: string;
        requestId: string;
        scheduledDeletionAt: string;
        cancellationLink: string;
    };
    meta: {
        timestamp: string;
    };
}

interface CancelDeletionResponse {
    data: {
        message: string;
        email?: string;
        cancelledAt?: string;
    };
    meta: {
        timestamp: string;
    };
}

interface ApiErrorData {
    error?: string;
    detail?: string;
}

type ApiError = AxiosError<ApiErrorData>;

export const useAccountDeletion = () => {
    const [isRequesting, setIsRequesting] = useState(false);
    const [isCancelling, setIsCancelling] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [requestSuccess, setRequestSuccess] = useState(false);
    const [cancelSuccess, setCancelSuccess] = useState(false);

    const getErrorMessage = useCallback((err: ApiError) =>
        err?.response?.data?.error ||
        err?.response?.data?.detail ||
        err?.message ||
        messageCatalog.deletion.genericError, []);

    const requestDeletion = useCallback(async (password: string) => {
        setIsRequesting(true);
        setError(null);
        setRequestSuccess(false);
        setCancelSuccess(false);

        try {
            const response = await apiClient.post(
                API_ROUTES.USERS.DELETION_REQUEST,
                { password }
            );

            const data: DeleteAccountResponse = response.data;

            setRequestSuccess(true);
            notify.success(messageCatalog.deletion.requestCreated);
            return data.data;

        } catch (err) {
            const error = err as ApiError;
            const message = getErrorMessage(error);
            setError(message);
            notify.error(message);
            throw err;

        } finally {
            setIsRequesting(false);
        }
    }, [getErrorMessage]);

    const cancelDeletion = useCallback(async (cancellationToken: string) => {
        setIsCancelling(true);
        setError(null);
        setCancelSuccess(false);

        try {
            const response = await apiClient.delete(
                API_ROUTES.USERS.CANCEL_DELETION,
                {
                    params: { token: cancellationToken }
                }
            );

            const data: CancelDeletionResponse = response.data;
            setCancelSuccess(true);
            notify.success(messageCatalog.deletion.requestCancelled);
            return data.data;
        } catch (err) {
            const error = err as ApiError;
            const errorCode = getApiErrorCode(error);
            const message =
                errorCode === 'DELETION_CANCEL_TOKEN_INVALID' || error?.response?.status === 400
                    ? messageCatalog.deletion.cancelTokenInvalid
                    : error?.response?.status === 409
                        ? messageCatalog.deletion.cancelUnavailable
                        : getErrorMessage(error);

            setError(message);
            notify.error(message);
            throw err;
        } finally {
            setIsCancelling(false);
        }
    }, [getErrorMessage]);

    return {
        requestDeletion,
        cancelDeletion,
        isRequesting,
        isCancelling,
        error,
        requestSuccess,
        cancelSuccess,
    };
};

import { useState, useCallback } from 'react';
import { AxiosError } from 'axios';
import { apiClient } from '../lib/api/apiClient';
import { API_ROUTES } from '@/lib/api/routes';

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
        'Có lỗi xảy ra', []);

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
            return data.data;

        } catch (err) {
            const error = err as ApiError;
            setError(getErrorMessage(error));
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
            return data.data;
        } catch (err) {
            const error = err as ApiError;
            const detail = error?.response?.data?.detail;
            const message =
                error?.response?.status === 400
                    ? (typeof detail === 'string' && detail.trim().length > 0
                        ? detail
                        : 'Liên kết hủy yêu cầu không hợp lệ hoặc đã hết hiệu lực.')
                    : error?.response?.status === 409
                        ? 'Yêu cầu xóa này không thể hủy được'
                        : getErrorMessage(error);

            setError(message);
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

import { client } from './client';

export interface PaymentRequest {
    courseId: string;
    paymentSystem: 'STRIPE_CARD' | 'STRIPE_PAYPAL' | 'PAYPAL';
    country: string;
    promoCode?: string;
}

export interface PaymentResponse {
    id: string;
    personId: string;
    personName: string;
    personEmail: string;
    courseId: string;
    courseName: string;
    amount: number;
    currency: string;
    paymentSystem: string;
    status: string;
    country: string;
    createdAt: string;
    accessDurationDays?: number | null;
    accessExpiresAt?: string | null;
}

export interface PaginatedResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    number: number;
    size: number;
}

export const processPayment = async (data: PaymentRequest): Promise<PaymentResponse> => {
    const response = await client.post<PaymentResponse>('/api/v1/payments/process', data);
    return response.data;
};

export const getAdminPayments = async (page = 0, size = 10, search = '', sortKey?: string, sortDir?: string, startDate?: string, endDate?: string): Promise<PaginatedResponse<PaymentResponse>> => {
    const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
        ...(search && { search }),
        ...(sortKey && { sortKey }),
        ...(sortDir && { sortDir }),
        ...(startDate && { startDate }),
        ...(endDate && { endDate })
    });
    const response = await client.get<PaginatedResponse<PaymentResponse>>(`/api/v1/payments/admin/all?${params.toString()}`);
    return response.data;
};

export const getMyPayments = async (page = 0, size = 10): Promise<PaginatedResponse<PaymentResponse>> => {
    const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString()
    });
    const response = await client.get<PaginatedResponse<PaymentResponse>>(`/api/v1/payments/my?${params.toString()}`);
    return response.data;
};

export const downloadPaymentReceipt = async (paymentId: string): Promise<Blob> => {
    const response = await client.get(`/api/v1/payments/${paymentId}/receipt`, {
        responseType: 'blob'
    });
    return response.data;
};

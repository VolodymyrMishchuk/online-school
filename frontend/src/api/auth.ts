import { client } from './client';

export interface AuthResponse {
    accessToken: string;
    userId: string;
    role: string;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export const login = async (request: LoginRequest): Promise<AuthResponse> => {
    const response = await client.post<AuthResponse>('/auth/login', request);
    return response.data;
};

export interface RegisterRequest {
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;
    bornedAt: string; // ISO string
    password: string;
}

export const register = async (request: RegisterRequest): Promise<void> => {
    await client.post('/auth/register', request);
};

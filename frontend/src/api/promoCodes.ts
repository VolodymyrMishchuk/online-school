import apiClient from './client';

export const PromoCodeStatus = {
    ACTIVE: 'ACTIVE',
    INACTIVE: 'INACTIVE'
} as const;
export type PromoCodeStatus = typeof PromoCodeStatus[keyof typeof PromoCodeStatus];

export const PromoCodeScope = {
    GLOBAL: 'GLOBAL',
    PERSONAL: 'PERSONAL'
} as const;
export type PromoCodeScope = typeof PromoCodeScope[keyof typeof PromoCodeScope];

export const DiscountType = {
    PERCENTAGE: 'PERCENTAGE',
    FIXED_AMOUNT: 'FIXED_AMOUNT',
    FIXED_PRICE: 'FIXED_PRICE'
} as const;
export type DiscountType = typeof DiscountType[keyof typeof DiscountType];

export interface PromoCodeDiscountDto {
    courseId: string | null;
    courseName?: string;
    discountType: DiscountType;
    discountValue: number;
    originalCoursePrice?: number;
}

export interface PromoCodeTargetUserDto {
    id: string;
    name: string;
    email: string;
    phone: string;
    usedCourseIds?: string[];
}

export interface PromoCodeResponseDto {
    id: string;
    code: string;
    status: PromoCodeStatus;
    scope: PromoCodeScope;
    targetPersons?: PromoCodeTargetUserDto[];
    validFrom?: string;
    validUntil?: string;
    validFromDisplay?: string;
    validUntilDisplay?: string;
    pendingActivation?: boolean;
    discounts: PromoCodeDiscountDto[];
}

export interface PromoCodeCreateFormDto {
    code: string;
    status: PromoCodeStatus;
    scope: PromoCodeScope;
    targetPersonIds?: string[];
    validFrom?: string | null;
    validUntil?: string | null;
    discounts: {
        courseId: string | null;
        discountType: DiscountType;
        discountValue: number;
    }[];
}

export interface CourseWithPromoDto {
    courseId: string;
    name: string;
    description: string;
    coverImageUrl?: string;
    averageColor?: string;
    price: number;
    promoPrice: number;
    discountType: DiscountType;
    discountValue: number;
    modulesCount: number;
    lessonsCount: number;
}

export interface PromoCodeCheckResponseDto {
    code: string;
    discounts: PromoCodeDiscountDto[];
    courses: CourseWithPromoDto[];
}

// Type matching Spring Data Page response
export interface PaginatedPromoCodesResponse {
    content: PromoCodeResponseDto[];
    pageable: any;
    totalElements: number;
    totalPages: number;
    last: boolean;
    size: number;
    number: number;
    sort: any;
    numberOfElements: number;
    first: boolean;
    empty: boolean;
}

export const promoCodesApi = {
    getPaginated: async (
        page: number,
        size: number,
        search?: string,
        sortKey?: string,
        sortDir?: 'asc' | 'desc',
        statusSort?: 'top' | 'bottom'
    ) => {
        const response = await apiClient.get<PaginatedPromoCodesResponse>('/promo-codes/paginated', {
            params: { page, size, search, sortKey, sortDir, statusSort }
        });
        return response.data;
    },

    create: async (data: PromoCodeCreateFormDto) => {
        const response = await apiClient.post<PromoCodeResponseDto>('/promo-codes', data);
        return response.data;
    },

    check: async (code: string) => {
        const response = await apiClient.get<PromoCodeCheckResponseDto>('/promo-codes/check', {
            params: { code }
        });
        return response.data;
    },

    update: async (id: string, data: PromoCodeCreateFormDto) => {
        const response = await apiClient.put<PromoCodeResponseDto>(`/promo-codes/${id}`, data);
        return response.data;
    },

    delete: async (id: string) => {
        await apiClient.delete(`/promo-codes/${id}`);
    },

    use: async (code: string, courseId: string) => {
        await apiClient.post('/promo-codes/use', null, {
            params: { code, courseId }
        });
    }
};

import { useState, useEffect } from 'react';

/**
 * Hook to automatically determine the user's country code (ISO Alpha-2).
 * It uses Cloudflare's edge trace API (https://1.1.1.1/cdn-cgi/trace) 
 * which is free, fast, and does not have strict rate limits like ipapi.co.
 * 
 * @param fallback - The default country code to return if detection fails.
 * @returns The detected or fallback country code.
 */
export const useCountryCode = (fallback: any = 'UA') => {
    const [countryCode, setCountryCode] = useState<any>(fallback);

    useEffect(() => {
        fetch('https://1.1.1.1/cdn-cgi/trace')
            .then(res => res.text())
            .then(text => {
                // Cloudflare trace returns key=value pairs, line by line
                const match = text.match(/loc=([A-Z]{2})/);
                if (match && match[1]) {
                    setCountryCode(match[1]);
                }
            })
            .catch(err => {
                console.warn('Failed to fetch country code, using fallback', err);
            });
    }, [fallback]);

    return countryCode;
};

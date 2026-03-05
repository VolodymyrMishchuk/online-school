import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import PhoneInput, { isValidPhoneNumber } from 'react-phone-number-input';
import 'react-phone-number-input/style.css';
import { register } from '../api/auth';
import { Heart } from 'lucide-react';

export default function RegisterPage() {
    const [formData, setFormData] = useState({
        firstName: '',
        lastName: '',
        email: '',
        phoneNumber: '',
        birthDate: '',
        password: ''
    });
    const [error, setError] = useState('');
    const navigate = useNavigate();
    const [defaultCountry, setDefaultCountry] = useState<any>('UA');

    useEffect(() => {
        fetch('https://ipapi.co/json/')
            .then(res => res.json())
            .then(data => {
                if (data && data.country_code) {
                    setDefaultCountry(data.country_code);
                }
            })
            .catch(err => console.error('Error fetching country:', err));
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (formData.phoneNumber && !isValidPhoneNumber(formData.phoneNumber)) {
            setError('Please enter a valid phone number');
            return;
        }

        try {
            // Format date to ISO OffsetDateTime
            const bornedAt = new Date(formData.birthDate).toISOString();

            const response = await register({
                firstName: formData.firstName,
                lastName: formData.lastName,
                email: formData.email,
                phoneNumber: formData.phoneNumber,
                bornedAt: bornedAt,
                password: formData.password
            });

            // Зберігаємо токен та дані користувача
            localStorage.setItem('token', response.accessToken);
            localStorage.setItem('userId', response.userId);
            localStorage.setItem('userRole', response.role);

            // Перенаправляємо на dashboard
            navigate('/dashboard');
        } catch (err: any) {
            const message = err.response?.data?.message || err.message;
            setError(message);
        }
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-brand-light/30 px-4 sm:px-6 lg:px-8 font-sans py-10">
            <div className="bg-white p-10 rounded-3xl shadow-xl w-full max-w-lg border border-brand-light">
                <div className="text-center mb-8">
                    <Link to="/" className="inline-block bg-brand-light p-3 rounded-full mb-4">
                        <Heart className="w-8 h-8 text-brand-secondary fill-brand-secondary" />
                    </Link>
                    <h2 className="text-3xl font-bold text-brand-dark">
                        Create Account
                    </h2>
                    <p className="mt-2 text-gray-500">Join our learning community today</p>
                </div>

                <form className="space-y-5" onSubmit={handleSubmit}>
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">First Name</label>
                            <input
                                type="text"
                                name="firstName"
                                required
                                className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white"
                                placeholder="John"
                                value={formData.firstName}
                                onChange={handleChange}
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">Last Name</label>
                            <input
                                type="text"
                                name="lastName"
                                required
                                className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white"
                                placeholder="Doe"
                                value={formData.lastName}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">Email</label>
                        <input
                            type="email"
                            name="email"
                            required
                            className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white"
                            placeholder="you@example.com"
                            value={formData.email}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">Phone</label>
                            <style>{`
                                .phone-input-override .PhoneInputInput {
                                    border: none;
                                    outline: none;
                                    background: transparent;
                                    width: 100%;
                                    color: inherit;
                                    height: 100%;
                                }
                                .phone-input-override .PhoneInputCountry {
                                    margin-right: 0.5rem;
                                }
                                .phone-input-override .PhoneInputCountrySelect {
                                    outline: none;
                                }
                            `}</style>
                            <div className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 focus-within:outline-none focus-within:ring-2 focus-within:ring-brand-primary/50 focus-within:border-brand-primary transition-all bg-gray-50 focus-within:bg-white">
                                <PhoneInput
                                    international
                                    defaultCountry={defaultCountry}
                                    value={formData.phoneNumber}
                                    onChange={(val) => setFormData({ ...formData, phoneNumber: val || '' })}
                                    placeholder="+1234567890"
                                    className="phone-input-override w-full"
                                />
                            </div>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">Birth Date</label>
                            <input
                                type="date"
                                name="birthDate"
                                required
                                className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white"
                                value={formData.birthDate}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">Password</label>
                        <input
                            type="password"
                            name="password"
                            required
                            className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white"
                            placeholder="••••••••"
                            value={formData.password}
                            onChange={handleChange}
                        />
                    </div>

                    {error && <div className="p-3 rounded-xl bg-red-50 text-red-500 text-sm text-center font-medium">{error}</div>}

                    <div>
                        <button
                            type="submit"
                            className="w-full py-3.5 px-4 border border-transparent rounded-full shadow-lg text-white bg-brand-primary hover:bg-brand-secondary focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-primary font-bold text-lg transition-all transform hover:-translate-y-0.5"
                        >
                            Sign Up
                        </button>
                    </div>
                </form>

                <div className="mt-6 text-center text-sm text-gray-500">
                    Already have an account? <Link to="/login" className="font-medium text-brand-primary hover:text-brand-secondary">Sign in</Link>
                </div>
            </div>
        </div>
    );
}

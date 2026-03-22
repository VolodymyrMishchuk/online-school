import { AdminPromoCodesPage } from './AdminPromoCodesPage';
import { UserPromoCodesPage } from './UserPromoCodesPage';

export default function PromoCodesRouter() {
    const userRole = localStorage.getItem('userRole') || 'USER';
    const isAdmin = userRole === 'ADMIN' || userRole === 'FAKE_ADMIN';

    return isAdmin ? <AdminPromoCodesPage /> : <UserPromoCodesPage />;
}

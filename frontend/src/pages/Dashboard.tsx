import { Box, Button, Card, CardContent, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { testsApi } from '../api/services';
import { useAuth } from '../auth/AuthContext';
import { StatusChip } from '../components/Chips';
import { ErrorState, Loading } from '../components/Feedback';
import { queryKeys } from '../hooks/useReferenceData';

export function Dashboard() {
  const { t } = useTranslation();
  const { user, isEditor } = useAuth();
  const { data, isLoading, error, refetch } = useQuery({ queryKey: queryKeys.summary, queryFn: testsApi.summary });
  return (
    <>
      <Typography variant="h4" gutterBottom>{t('dashboard.title')}</Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        {t('dashboard.hello', { name: user?.name })} <strong>{t(`roles.${user?.role ?? 'READER'}`)}</strong>{isEditor ? '.' : ` ${t('dashboard.readOnly')}`}
      </Typography>
      {isLoading && <Loading />}
      {error && <ErrorState error={error} onRetry={() => refetch()} />}
      {data && (
        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(6, 1fr)' } }}>
          <Card><CardContent><Typography variant="h3" component="div">{data.total}</Typography><Typography color="text.secondary">{t('dashboard.tests')}</Typography></CardContent></Card>
          {Object.entries(data.byStatus).map(([status, count]) => (
            <Card key={status} component={Link} to={`/tests?status=${encodeURIComponent(status)}`} sx={{ textDecoration: 'none' }}>
              <CardContent><Typography variant="h3" component="div">{count}</Typography><StatusChip status={status} /></CardContent>
            </Card>
          ))}
        </Box>
      )}
      <Stack direction="row" spacing={2} sx={{ mt: 4 }} flexWrap="wrap" useFlexGap>
        <Button variant="contained" component={Link} to="/tests">{t('dashboard.openOverview')}</Button>
        {isEditor && <Button variant="outlined" component={Link} to="/tests/new">{t('dashboard.createTest')}</Button>}
        <Button variant="outlined" component={Link} to="/test-types">{t('dashboard.types')}</Button>
      </Stack>
    </>
  );
}

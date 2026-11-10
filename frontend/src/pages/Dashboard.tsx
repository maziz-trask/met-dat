import { Box, Button, Card, CardContent, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { testsApi } from '../api/services';
import { useAuth } from '../auth/AuthContext';
import { StatusChip } from '../components/Chips';
import { ErrorState, Loading } from '../components/Feedback';

export function Dashboard() {
  const { user, isEditor } = useAuth();
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['summary'], queryFn: testsApi.summary });
  return (
    <>
      <Typography variant="h4" gutterBottom>Dashboard</Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Hello {user?.name}. You are working as <strong>{isEditor ? 'Editor' : 'Reader'}</strong>{isEditor ? '.' : ' (read-only).'}
      </Typography>
      {isLoading && <Loading />}
      {error && <ErrorState error={error} onRetry={() => refetch()} />}
      {data && (
        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(6, 1fr)' } }}>
          <Card><CardContent><Typography variant="h3" component="div">{data.total}</Typography><Typography color="text.secondary">Technical tests</Typography></CardContent></Card>
          {Object.entries(data.byStatus).map(([s, n]) => (
            <Card key={s} component={Link} to={`/tests?status=${encodeURIComponent(s)}`} sx={{ textDecoration: 'none' }}>
              <CardContent><Typography variant="h3" component="div">{n}</Typography><StatusChip status={s} /></CardContent>
            </Card>
          ))}
        </Box>
      )}
      <Stack direction="row" spacing={2} sx={{ mt: 4 }} flexWrap="wrap" useFlexGap>
        <Button variant="contained" component={Link} to="/tests">Open test overview</Button>
        {isEditor && <Button variant="outlined" component={Link} to="/tests/new">Create a new test</Button>}
        <Button variant="outlined" component={Link} to="/test-types">Test types</Button>
      </Stack>
    </>
  );
}

import { createBrowserRouter, Navigate } from 'react-router'

import { Appointment } from '@/screens/Appointment'
import { Complete } from '@/screens/Complete'
import { HaveSelect } from '@/screens/HaveSelect'
import { Home } from '@/screens/Home'
import { Identify } from '@/screens/Identify'
import { MatchResult } from '@/screens/MatchResult'
import { NeedsSelect } from '@/screens/NeedsSelect'
import { Onboarding } from '@/screens/Onboarding'
import { PlaceView } from '@/screens/PlaceView'
import { PokeConfirm } from '@/screens/PokeConfirm'
import { PokeReceived } from '@/screens/PokeReceived'
import { TimeSelect } from '@/screens/TimeSelect'

import { AppShell } from './AppShell'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    children: [
      { index: true, element: <Onboarding /> },
      { path: 'have', element: <HaveSelect /> },
      { path: 'needs', element: <NeedsSelect /> },
      { path: 'home', element: <Home /> },
      { path: 'poke/confirm', element: <PokeConfirm /> },
      { path: 'poke/received', element: <PokeReceived /> },
      { path: 'match', element: <MatchResult /> },
      { path: 'place', element: <PlaceView /> },
      { path: 'time', element: <TimeSelect /> },
      { path: 'appointment', element: <Appointment /> },
      { path: 'identify', element: <Identify /> },
      { path: 'complete', element: <Complete /> },
      // 없는 주소로 들어와도 갇히지 않는다.
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
])

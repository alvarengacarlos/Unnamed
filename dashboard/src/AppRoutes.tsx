import { lazy } from 'react';
import { Route, Routes } from 'react-router';
import { RoutePath } from './common/common.type';
import App from './App';

// Each page is a chunk of its own, downloaded the first time it is opened
const Dashboard = lazy(() => import('./dashboard/Dashboard'))
const Rank = lazy(() => import('./rank/Rank'))
const Member = lazy(() => import('./member/Member'))
const Tuition = lazy(() => import('./tuition/Tuition'))
const Clazz = lazy(() => import('./clazz/Clazz'))
const Grading = lazy(() => import('./grading/Grading'))
const Tournament = lazy(() => import('./tournament/Tournament'))
const Assistant = lazy(() => import('./assistant/Assistant'))

export default function AppRoutes() {
    return (
        <Routes>
            <Route element={<App/>}>
                <Route index element={<Dashboard/>}/>
                <Route path={RoutePath.RANK} element={<Rank/>}/>
                <Route path={RoutePath.MEMBER} element={<Member/>}/>
                <Route path={RoutePath.TUITION} element={<Tuition/>}/>
                <Route path={RoutePath.CLAZZ} element={<Clazz/>}/>
                <Route path={RoutePath.GRADING} element={<Grading/>}/>
                <Route path={RoutePath.TOURNAMENT} element={<Tournament/>}/>
                <Route path={RoutePath.ASSISTANT} element={<Assistant/>}/>
            </Route>
        </Routes>
    )
}

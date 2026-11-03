/**
 * App.jsx - Componente raíz con layout principal en tonos verde claro.
 */
import { useState } from 'react'
import Sidebar          from './components/Sidebar'
import InformacionPage  from './pages/InformacionPage'
import CountingSortPage from './pages/CountingSortPage'
import BucketSortPage   from './pages/BucketSortPage'
import RadixSortPage    from './pages/RadixSortPage'
import ComparacionPage from './pages/ComparacionPage'
import JaccardPage from './pages/JaccardPage'
import DicePage from './pages/DicePage'
import LevenshteinPage from './pages/LevenshteinPage'
import CosenoTfidfPage from './pages/CosenoTfidfPage'
import ComparacionClasicaPage from './pages/ComparacionClasicaPage'
import MiniLmPage from './pages/MiniLmPage'
import BgePage from './pages/BgePage'
import ClusteringPage from './pages/ClusteringPage'
import BusquedaExactaPage from './pages/BusquedaExactaPage'
import './App.css'

function App() {
  const [ventanaActiva, setVentanaActiva]   = useState('informacion')
  const [sidebarAbierto, setSidebarAbierto] = useState(true)

  return (
    <div className="layout-principal">
      <Sidebar
        ventanaActiva={ventanaActiva}
        setVentanaActiva={setVentanaActiva}
        totalArticulos={20}
        isOpen={sidebarAbierto}
        setIsOpen={setSidebarAbierto}
      />

      <div className={`area-principal ${sidebarAbierto ? '' : 'sidebar-cerrado'}`}>
        <main className="contenido-vista">
          {ventanaActiva === 'informacion'  && <InformacionPage />}
          {ventanaActiva === 'countingsort' && <CountingSortPage />}
          {ventanaActiva === 'bucketsort'   && <BucketSortPage />}
          {ventanaActiva === 'radixsort'    && <RadixSortPage />}
          {ventanaActiva === 'comparacion' && <ComparacionPage />}
          {ventanaActiva === 'jaccard' && <JaccardPage />}
          {ventanaActiva === 'dice' && <DicePage />}
          {ventanaActiva === 'levenshtein' && <LevenshteinPage />}
          {ventanaActiva === 'coseno-tfidf' && <CosenoTfidfPage />}
          {ventanaActiva === 'comparacion-clasica' && <ComparacionClasicaPage />}
          {ventanaActiva === 'minilm' && <MiniLmPage />}
          {ventanaActiva === 'bge' && <BgePage />}
          {ventanaActiva === 'clustering' && <ClusteringPage />}
          {ventanaActiva === 'busqueda-exacta' && <BusquedaExactaPage />}
        </main>
      </div>
    </div>
  )
}

export default App
